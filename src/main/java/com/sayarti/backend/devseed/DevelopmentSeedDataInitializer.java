package com.sayarti.backend.devseed;

import com.sayarti.backend.expense.entity.Expense;
import com.sayarti.backend.expense.entity.ExpenseCategory;
import com.sayarti.backend.expense.repository.ExpenseRepository;
import com.sayarti.backend.fuel.entity.FuelRecord;
import com.sayarti.backend.fuel.repository.FuelRecordRepository;
import com.sayarti.backend.maintenance.entity.MaintenanceCategory;
import com.sayarti.backend.maintenance.entity.MaintenanceRecord;
import com.sayarti.backend.maintenance.repository.MaintenanceRecordRepository;
import com.sayarti.backend.reference.entity.Country;
import com.sayarti.backend.reference.repository.CountryRepository;
import com.sayarti.backend.reference.repository.CurrencyRepository;
import com.sayarti.backend.reminder.entity.Reminder;
import com.sayarti.backend.reminder.entity.ReminderCategory;
import com.sayarti.backend.reminder.entity.ReminderTriggerType;
import com.sayarti.backend.reminder.repository.ReminderRepository;
import com.sayarti.backend.user.entity.User;
import com.sayarti.backend.user.repository.UserRepository;
import com.sayarti.backend.vehicle.entity.FuelType;
import com.sayarti.backend.vehicle.entity.PowertrainType;
import com.sayarti.backend.vehicle.entity.Vehicle;
import com.sayarti.backend.vehicle.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevelopmentSeedDataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DevelopmentSeedDataInitializer.class);

    private final UserRepository users;
    private final CountryRepository countries;
    private final CurrencyRepository currencies;
    private final VehicleRepository vehicles;
    private final FuelRecordRepository fuelRecords;
    private final MaintenanceRecordRepository maintenanceRecords;
    private final ExpenseRepository expenses;
    private final ReminderRepository reminders;
    private final PasswordEncoder passwordEncoder;
    private final String password;

    public DevelopmentSeedDataInitializer(UserRepository users, CountryRepository countries,
            CurrencyRepository currencies, VehicleRepository vehicles,
            FuelRecordRepository fuelRecords, MaintenanceRecordRepository maintenanceRecords,
            ExpenseRepository expenses, ReminderRepository reminders,
            PasswordEncoder passwordEncoder,
            @Value("${sayarti.dev-seed.password:}") String password) {
        this.users = users;
        this.countries = countries;
        this.currencies = currencies;
        this.vehicles = vehicles;
        this.fuelRecords = fuelRecords;
        this.maintenanceRecords = maintenanceRecords;
        this.expenses = expenses;
        this.reminders = reminders;
        this.passwordEncoder = passwordEncoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (password == null || password.isBlank()) {
            log.warn("Development seed users were skipped because DEV_SEED_PASSWORD is blank.");
            return;
        }
        seedUser("naserjordan@gmail.com", "Naser", "Al-Khatib", "JO", "JOD",
                jordanVehicles());
        seedUser("naserksa@gmail.com", "Naser", "Al-Harbi", "SA", "SAR",
                saudiVehicles());
        log.info("Development seed data initialized.");
    }

    private void seedUser(String email, String firstName, String lastName, String countryCode,
            String currencyCode, List<VehicleTemplate> templates) {
        Country country = countries.findByCodeAndActiveTrue(countryCode).orElse(null);
        if (country == null || !country.getDefaultCurrencyCode().equals(currencyCode)
                || currencies.findByCodeAndActiveTrue(currencyCode).isEmpty()) {
            log.warn("Development seed user {} was skipped because reference data is unavailable.", email);
            return;
        }

        User user = users.findByEmailIgnoreCase(email).orElseGet(() -> {
            User created = new User(firstName, lastName, email, passwordEncoder.encode(password),
                    countryCode, currencyCode);
            created.verifyEmail();
            return users.save(created);
        });
        if (user.getDeletedAt() != null) {
            log.warn("Development seed user {} was skipped because that email is soft-deleted.", email);
            return;
        }
        for (VehicleTemplate template : templates) {
            Vehicle vehicle = vehicles.findByUserIdAndLicensePlate(
                    user.getId(), template.plate()).orElseGet(() -> vehicles.save(template.create(user)));
            if (vehicle.getDeletedAt() == null) {
                seedVehicleData(vehicle, currencyCode, template.fuelPowered());
            }
        }
    }

    private void seedVehicleData(Vehicle vehicle, String currency, boolean fuelPowered) {
        Instant now = Instant.now();
        if (fuelPowered) {
            long[] distances = {2100, 1680, 1240, 790, 340};
            String[] stations = {"TotalEnergies", "Manaseer", "JoPetrol", "Aldrees", "SASCO"};
            for (int index = 0; index < distances.length; index++) {
                BigDecimal odometer = BigDecimal.valueOf(vehicle.getCurrentMileage() - distances[index]);
                if (!fuelRecords.existsByVehicleIdAndOdometerKm(vehicle.getId(), odometer)) {
                    BigDecimal liters = BigDecimal.valueOf(38 + index * 1.35);
                    BigDecimal price = currency.equals("JOD")
                            ? BigDecimal.valueOf(1.02 + index * .005)
                            : BigDecimal.valueOf(2.18 + index * .02);
                    fuelRecords.save(new FuelRecord(vehicle.getId(), odometer, liters, price,
                            liters.multiply(price), currency, now.minus(75L - index * 15L, ChronoUnit.DAYS),
                            true, stations[index], "Development full-tank refill"));
                }
            }
        }

        if (vehicle.getPowertrainType() == PowertrainType.ELECTRIC) {
            seedMaintenance(vehicle, currency, "Seed EV battery inspection", MaintenanceCategory.BATTERY,
                    90, 450, now.minus(110, ChronoUnit.DAYS));
        } else {
            seedMaintenance(vehicle, currency, "Seed oil and filter service", MaintenanceCategory.OIL_CHANGE,
                    160, 1800, now.minus(110, ChronoUnit.DAYS));
        }
        seedMaintenance(vehicle, currency, "Seed brake inspection", MaintenanceCategory.BRAKE_SERVICE,
                240, 950, now.minus(64, ChronoUnit.DAYS));
        seedMaintenance(vehicle, currency, "Seed tire rotation", MaintenanceCategory.TIRE_SERVICE,
                75, 420, now.minus(28, ChronoUnit.DAYS));

        seedExpense(vehicle, currency, "Seed annual insurance", ExpenseCategory.INSURANCE,
                460, 2100, now.minus(140, ChronoUnit.DAYS));
        seedExpense(vehicle, currency, "Seed vehicle registration", ExpenseCategory.REGISTRATION,
                65, 350, now.minus(95, ChronoUnit.DAYS));
        seedExpense(vehicle, currency, "Seed premium car wash", ExpenseCategory.WASH,
                12, 55, now.minus(12, ChronoUnit.DAYS));

        seedReminder(vehicle, "Seed upcoming insurance renewal", ReminderCategory.INSURANCE_EXPIRATION,
                ReminderTriggerType.DATE, now.plus(60, ChronoUnit.DAYS), null, false);
        seedReminder(vehicle, "Seed next general service", ReminderCategory.MAINTENANCE,
                ReminderTriggerType.MILEAGE, null, vehicle.getCurrentMileage() + 5000, false);
        seedReminder(vehicle, "Seed completed registration renewal", ReminderCategory.REGISTRATION,
                ReminderTriggerType.DATE, now.minus(30, ChronoUnit.DAYS), null, true);
    }

    private void seedMaintenance(Vehicle vehicle, String currency, String title,
            MaintenanceCategory category, int jodCost, int sarCost, Instant date) {
        if (!maintenanceRecords.existsByVehicleIdAndTitle(vehicle.getId(), title)) {
            maintenanceRecords.save(new MaintenanceRecord(vehicle.getId(), category, title, date,
                    BigDecimal.valueOf(vehicle.getCurrentMileage() - 2500),
                    BigDecimal.valueOf(currency.equals("JOD") ? jodCost : sarCost), currency,
                    "Authorized service center", "Development maintenance history"));
        }
    }

    private void seedExpense(Vehicle vehicle, String currency, String title,
            ExpenseCategory category, int jodAmount, int sarAmount, Instant date) {
        if (!expenses.existsByVehicleIdAndTitle(vehicle.getId(), title)) {
            expenses.save(new Expense(vehicle.getId(), category, title, date,
                    BigDecimal.valueOf(currency.equals("JOD") ? jodAmount : sarAmount), currency,
                    "Development expense history"));
        }
    }

    private void seedReminder(Vehicle vehicle, String title, ReminderCategory category,
            ReminderTriggerType trigger, Instant date, Long mileage, boolean completed) {
        if (!reminders.existsByVehicleIdAndTitle(vehicle.getId(), title)) {
            Reminder reminder = new Reminder(vehicle.getId(), category, title,
                    "Development reminder", trigger, date, mileage);
            if (completed) {
                reminder.complete();
            }
            reminders.save(reminder);
        }
    }

    private List<VehicleTemplate> jordanVehicles() {
        return List.of(
                new VehicleTemplate("Toyota", "Corolla", 2021, PowertrainType.GASOLINE, 68420,
                        "DEV-JO-101", "Amman Commuter", FuelType.GASOLINE_95, "50", null, null),
                new VehicleTemplate("Hyundai", "Ioniq 5", 2023, PowertrainType.ELECTRIC, 28750,
                        "DEV-JO-202", "Electric Explorer", null, null, "77.4", "480"));
    }

    private List<VehicleTemplate> saudiVehicles() {
        return List.of(
                new VehicleTemplate("Toyota", "Land Cruiser", 2022, PowertrainType.GASOLINE, 75200,
                        "DEV-SA-303", "Desert Cruiser", FuelType.GASOLINE_95, "110", null, null),
                new VehicleTemplate("Toyota", "Camry Hybrid", 2024, PowertrainType.HYBRID, 31900,
                        "DEV-SA-404", "Riyadh Hybrid", FuelType.GASOLINE_95, "50", "1.6", "850"));
    }

    private record VehicleTemplate(String brand, String model, int year, PowertrainType powertrain,
            long mileage, String plate, String nickname, FuelType fuelType, String tank,
            String battery, String range) {
        Vehicle create(User user) {
            return new Vehicle(user.getId(), brand, model, year, powertrain, mileage, plate,
                    nickname, null, fuelType, decimal(tank), decimal(battery), decimal(range));
        }

        boolean fuelPowered() {
            return fuelType != null;
        }

        private BigDecimal decimal(String value) {
            return value == null ? null : new BigDecimal(value);
        }
    }
}
