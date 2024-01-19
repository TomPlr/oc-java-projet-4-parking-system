package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static com.parkit.parkingsystem.constants.Fare.CAR_RATE_PER_HOUR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    }


    @Test
    public void testParkingACar(){
        parkingService.processIncomingVehicle();

        Ticket ticket = ticketDAO.getTicket("ABCDEF");

        assertNotNull(ticket);
        assertEquals(1,ticket.getId());
        assertFalse(ticket.getParkingSpot().isAvailable());
    }

    @Test
    public void testParkingLotExit(){
        Ticket ticket = new Ticket();
        int parkingSlot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);

        ParkingSpot parkingSpot = new ParkingSpot(parkingSlot, ParkingType.CAR, false);

        ticket.setId(1);
        ticket.setParkingSpot(parkingSpot);
        ticket.setInTime(Date.from(
                LocalDateTime.now().minusHours(1).truncatedTo(ChronoUnit.SECONDS).atZone(ZoneId.systemDefault()).toInstant()
        ));
        ticket.setVehicleRegNumber("ABCDEF");

        ticketDAO.saveTicket(ticket);

        parkingService.processExitingVehicle();


        assertNotNull(ticketDAO.getTicket("ABCDEF").getOutTime());
        assertEquals(CAR_RATE_PER_HOUR ,ticketDAO.getTicket("ABCDEF").getPrice());
    }

    @Test
    public void testParkingLotExitingWithDiscount(){
        Ticket firstTicket = new Ticket();
        int parkingSlot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);

        ParkingSpot parkingSpot = new ParkingSpot(parkingSlot, ParkingType.CAR, false);

        firstTicket.setId(1);
        firstTicket.setParkingSpot(parkingSpot);
        firstTicket.setInTime(Date.from(
                LocalDateTime.now().minusHours(3).truncatedTo(ChronoUnit.SECONDS).atZone(ZoneId.systemDefault()).toInstant()
        ));
        firstTicket.setOutTime(Date.from(
                LocalDateTime.now().minusHours(2).truncatedTo(ChronoUnit.SECONDS).atZone(ZoneId.systemDefault()).toInstant()
        ));
        firstTicket.setVehicleRegNumber("ABCDEF");

        ticketDAO.saveTicket(firstTicket);

        Ticket ticket = new Ticket();
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");

        ticket.setId(2);
        ticket.setInTime(Date.from(
                LocalDateTime.now().minusHours(1).truncatedTo(ChronoUnit.SECONDS).atZone(ZoneId.systemDefault()).toInstant()
        ));
        ticketDAO.saveTicket(ticket);

        parkingService.processExitingVehicle();

        assertEquals(CAR_RATE_PER_HOUR * 0.95 ,ticketDAO.getTicket("ABCDEF").getPrice());
    }
}
