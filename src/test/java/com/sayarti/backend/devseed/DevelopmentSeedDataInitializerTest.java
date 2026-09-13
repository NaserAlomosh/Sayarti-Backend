package com.sayarti.backend.devseed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sayarti.backend.expense.entity.Expense;
import com.sayarti.backend.expense.repository.ExpenseRepository;
import com.sayarti.backend.fuel.entity.FuelRecord;
import com.sayarti.backend.fuel.repository.FuelRecordRepository;
import com.sayarti.backend.maintenance.entity.MaintenanceRecord;
import com.sayarti.backend.maintenance.repository.MaintenanceRecordRepository;
import com.sayarti.backend.reference.entity.Country;
import com.sayarti.backend.reference.entity.Currency;
import com.sayarti.backend.reference.repository.CountryRepository;
import com.sayarti.backend.reference.repository.CurrencyRepository;
import com.sayarti.backend.reminder.entity.Reminder;
import com.sayarti.backend.reminder.repository.ReminderRepository;
import com.sayarti.backend.user.entity.AuthProvider;
import com.sayarti.backend.user.entity.User;
import com.sayarti.backend.user.repository.UserRepository;
import com.sayarti.backend.vehicle.entity.Vehicle;
import com.sayarti.backend.vehicle.repository.VehicleRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class DevelopmentSeedDataInitializerTest {
    @Test
    void isRestrictedToTheDevProfile() {
        Profile profile = DevelopmentSeedDataInitializer.class.getAnnotation(Profile.class);
        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("dev & !test");
    }

    @Test
    void createsRealisticVerifiedDataAndIsIdempotent() throws Exception {
        Fixture fixture = new Fixture();
        DevelopmentSeedDataInitializer initializer = fixture.initializer("local-dev-secret");

        initializer.run(new DefaultApplicationArguments());
        initializer.run(new DefaultApplicationArguments());

        assertThat(fixture.usersByEmail).hasSize(2);
        User jordan = fixture.usersByEmail.get("naserjordan@gmail.com");
        User saudi = fixture.usersByEmail.get("naserksa@gmail.com");
        assertThat(jordan.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(jordan.isEmailVerified()).isTrue();
        assertThat(jordan.getCountryCode()).isEqualTo("JO");
        assertThat(jordan.getDefaultCurrencyCode()).isEqualTo("JOD");
        assertThat(saudi.isEmailVerified()).isTrue();
        assertThat(saudi.getCountryCode()).isEqualTo("SA");
        assertThat(saudi.getDefaultCurrencyCode()).isEqualTo("SAR");
        assertThat(jordan.getPasswordHash()).isNotEqualTo("local-dev-secret");
        assertThat(new BCryptPasswordEncoder().matches("local-dev-secret", jordan.getPasswordHash())).isTrue();

        assertThat(fixture.vehicles).hasSize(4);
        assertThat(fixture.vehicles).filteredOn(v -> v.getUserId().equals(jordan.getId())).hasSize(2);
        assertThat(fixture.vehicles).filteredOn(v -> v.getUserId().equals(saudi.getId())).hasSize(2);
        assertThat(fixture.fuel).hasSize(15).allMatch(r -> r.getDeletedAt() == null);
        assertThat(fixture.maintenance).hasSize(12).allMatch(r -> r.getMileageKm().signum() >= 0);
        assertThat(fixture.expenses).hasSize(12).allMatch(e -> e.getAmount().signum() > 0);
        assertThat(fixture.reminders).hasSize(12);
        assertThat(fixture.reminders).anyMatch(Reminder::isCompleted)
                .anyMatch(reminder -> !reminder.isCompleted());
        assertThat(fixture.reminders).allMatch(r -> r.getNotificationDeliveredAt() == null);
    }

    @Test
    void blankPasswordSkipsAllSeeding() throws Exception {
        Fixture fixture = new Fixture();
        fixture.initializer("  ").run(new DefaultApplicationArguments());
        assertThat(fixture.usersByEmail).isEmpty();
        assertThat(fixture.vehicles).isEmpty();
    }

    @Test
    void doesNotOverwriteAnExistingUser() throws Exception {
        Fixture fixture = new Fixture();
        User existing = new User("Owner", "Managed", "naserjordan@gmail.com",
                "existing-hash", "US", "USD");
        fixture.usersByEmail.put(existing.getEmail(), existing);
        fixture.initializer("local-dev-secret").run(new DefaultApplicationArguments());
        assertThat(existing.getFirstName()).isEqualTo("Owner");
        assertThat(existing.getCountryCode()).isEqualTo("US");
        assertThat(existing.getPasswordHash()).isEqualTo("existing-hash");
    }

    @Test
    void doesNotResurrectOrSeedDataForASoftDeletedUser() throws Exception {
        Fixture fixture = new Fixture();
        User deleted = new User("Former", "Owner", "naserjordan@gmail.com",
                "existing-hash", "JO", "JOD");
        deleted.delete();
        fixture.usersByEmail.put(deleted.getEmail(), deleted);

        fixture.initializer("local-dev-secret").run(new DefaultApplicationArguments());

        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThat(fixture.vehicles).noneMatch(v -> v.getUserId().equals(deleted.getId()));
    }

    private static final class Fixture {
        final UserRepository userRepository = mock(UserRepository.class);
        final CountryRepository countryRepository = mock(CountryRepository.class);
        final CurrencyRepository currencyRepository = mock(CurrencyRepository.class);
        final VehicleRepository vehicleRepository = mock(VehicleRepository.class);
        final FuelRecordRepository fuelRepository = mock(FuelRecordRepository.class);
        final MaintenanceRecordRepository maintenanceRepository = mock(MaintenanceRecordRepository.class);
        final ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        final ReminderRepository reminderRepository = mock(ReminderRepository.class);
        final Map<String, User> usersByEmail = new HashMap<>();
        final List<Vehicle> vehicles = new ArrayList<>();
        final List<FuelRecord> fuel = new ArrayList<>();
        final List<MaintenanceRecord> maintenance = new ArrayList<>();
        final List<Expense> expenses = new ArrayList<>();
        final List<Reminder> reminders = new ArrayList<>();

        Fixture() throws Exception {
            Country jordan = entity(Country.class, Map.of("code", "JO", "defaultCurrencyCode", "JOD", "active", true));
            Country saudi = entity(Country.class, Map.of("code", "SA", "defaultCurrencyCode", "SAR", "active", true));
            Currency jod = entity(Currency.class, Map.of("code", "JOD", "active", true));
            Currency sar = entity(Currency.class, Map.of("code", "SAR", "active", true));
            when(countryRepository.findByCodeAndActiveTrue("JO")).thenReturn(Optional.of(jordan));
            when(countryRepository.findByCodeAndActiveTrue("SA")).thenReturn(Optional.of(saudi));
            when(currencyRepository.findByCodeAndActiveTrue("JOD")).thenReturn(Optional.of(jod));
            when(currencyRepository.findByCodeAndActiveTrue("SAR")).thenReturn(Optional.of(sar));
            when(userRepository.findByEmailIgnoreCase(any())).thenAnswer(i -> Optional.ofNullable(usersByEmail.get(i.getArgument(0))));
            when(userRepository.save(any())).thenAnswer(i -> {
                User user = i.getArgument(0); usersByEmail.put(user.getEmail(), user); return user;
            });
            when(vehicleRepository.findByUserIdAndLicensePlate(any(), any())).thenAnswer(i ->
                    vehicles.stream().filter(v -> v.getUserId().equals(i.getArgument(0))
                            && v.getLicensePlate().equals(i.getArgument(1))).findFirst());
            when(vehicleRepository.save(any())).thenAnswer(i -> { Vehicle v = i.getArgument(0); vehicles.add(v); return v; });
            when(fuelRepository.existsByVehicleIdAndOdometerKm(any(), any())).thenAnswer(i ->
                    fuel.stream().anyMatch(r -> r.getVehicleId().equals(i.getArgument(0))
                            && r.getOdometerKm().compareTo(i.getArgument(1, BigDecimal.class)) == 0));
            when(fuelRepository.save(any())).thenAnswer(i -> { FuelRecord r = i.getArgument(0); fuel.add(r); return r; });
            when(maintenanceRepository.existsByVehicleIdAndTitle(any(), any())).thenAnswer(i ->
                    maintenance.stream().anyMatch(r -> r.getVehicleId().equals(i.getArgument(0)) && r.getTitle().equals(i.getArgument(1))));
            when(maintenanceRepository.save(any())).thenAnswer(i -> { MaintenanceRecord r = i.getArgument(0); maintenance.add(r); return r; });
            when(expenseRepository.existsByVehicleIdAndTitle(any(), any())).thenAnswer(i ->
                    expenses.stream().anyMatch(e -> e.getVehicleId().equals(i.getArgument(0)) && e.getTitle().equals(i.getArgument(1))));
            when(expenseRepository.save(any())).thenAnswer(i -> { Expense e = i.getArgument(0); expenses.add(e); return e; });
            when(reminderRepository.existsByVehicleIdAndTitle(any(), any())).thenAnswer(i ->
                    reminders.stream().anyMatch(r -> r.getVehicleId().equals(i.getArgument(0)) && r.getTitle().equals(i.getArgument(1))));
            when(reminderRepository.save(any())).thenAnswer(i -> { Reminder r = i.getArgument(0); reminders.add(r); return r; });
        }

        DevelopmentSeedDataInitializer initializer(String password) {
            return new DevelopmentSeedDataInitializer(userRepository, countryRepository,
                    currencyRepository, vehicleRepository, fuelRepository, maintenanceRepository,
                    expenseRepository, reminderRepository, new BCryptPasswordEncoder(), password);
        }

        private static <T> T entity(Class<T> type, Map<String, Object> fields) throws Exception {
            var constructor = type.getDeclaredConstructor(); constructor.setAccessible(true);
            T result = constructor.newInstance();
            for (var entry : fields.entrySet()) {
                Field field = type.getDeclaredField(entry.getKey()); field.setAccessible(true);
                field.set(result, entry.getValue());
            }
            return result;
        }
    }
}
