package com.projet.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import com.projet.entity.CollectPoint;
import com.projet.entity.Tournee;
import com.projet.entity.Vehicle;
import com.projet.repository.CollectPointRepository;
import com.projet.repository.TourneeRepository;
import com.projet.repository.VehicleRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TourneeServiceTest {

    @Mock
    TourneeRepository tourRepo;

    @Mock
    CollectPointRepository cpRepo;

    @Mock
    VehicleRepository vehicleRepo;

    @InjectMocks
    TourneeService service;

    @Test
    void planifierIntelligent_picksMostFilledWithinCapacity() {
        Vehicle v = new Vehicle();
        v.setId("veh-1");
        v.setAvailable(true);
        v.setCapacity(1000);
        v.setLatitude(36.8);
        v.setLongitude(10.18);

        CollectPoint p1 = new CollectPoint(); p1.setId("p1"); p1.setCapacityLiters(600); p1.setMaxCapacityLiters(800); p1.setLatitude(36.81); p1.setLongitude(10.19);
        CollectPoint p2 = new CollectPoint(); p2.setId("p2"); p2.setCapacityLiters(400); p2.setMaxCapacityLiters(1000); p2.setLatitude(36.82); p2.setLongitude(10.20);
        CollectPoint p3 = new CollectPoint(); p3.setId("p3"); p3.setCapacityLiters(300); p3.setMaxCapacityLiters(300); p3.setLatitude(36.805); p3.setLongitude(10.185);
        CollectPoint p4 = new CollectPoint(); p4.setId("p4"); p4.setCapacityLiters(700); p4.setMaxCapacityLiters(1000); p4.setLatitude(36.9); p4.setLongitude(10.3);

        when(vehicleRepo.findAll()).thenReturn(List.of(v));
        when(cpRepo.findAll()).thenReturn(Arrays.asList(p1,p2,p3,p4));
        when(tourRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(vehicleRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        Tournee result = service.planifierIntelligent();

        assertNotNull(result);
        assertEquals("veh-1", result.getVehicleId());
        // expected picks: p3 (100% -> 300), then p1 (75% -> 600) -> remaining 100 can't take p2 or p4
        assertEquals(2, result.getCollectPoints().size());
        assertEquals("p3", result.getCollectPoints().get(0));
        assertEquals("p1", result.getCollectPoints().get(1));

        verify(vehicleRepo).save(any());
        verify(tourRepo).save(any());
    }
}
