package com.diving.pungdong.service;

import com.diving.pungdong.advice.exception.BadRequestException;
import com.diving.pungdong.advice.exception.ReservationFullException;
import com.diving.pungdong.domain.account.Account;
import com.diving.pungdong.domain.payment.Payment;
import com.diving.pungdong.domain.reservation.Reservation;
import com.diving.pungdong.domain.schedule.Schedule;
import com.diving.pungdong.domain.schedule.ScheduleDetail;
import com.diving.pungdong.domain.schedule.ScheduleTime;
import com.diving.pungdong.dto.reservation.ReservationCreateReq;
import com.diving.pungdong.dto.reservation.ReservationDateDto;
import com.diving.pungdong.repo.ReservationJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {
    private final ReservationJpaRepo reservationJpaRepo;
    private final ScheduleService scheduleService;
    private final ScheduleDetailService scheduleDetailService;
    private final ReservationDateService reservationDateService;
    private final PaymentService paymentService;

    public Reservation makeReservation(Account account, ReservationCreateReq req) {
        Schedule schedule = scheduleService.getScheduleById(req.getScheduleId());
        if (!scheduleService.checkValidReservationDate(schedule.getScheduleDetails(), req.getReservationDateList())) {
            throw new BadRequestException();
        }

        if (scheduleService.isReservationFull(schedule, req.getReservationDateList())) {
            throw new ReservationFullException();
        }

        Integer cost = paymentService.calcCost(req.getEquipmentList(), schedule.getLecture());
        Payment payment = paymentService.savePayment(cost);

        Reservation reservation = saveReservation(account, schedule, payment, req);
        reservationDateService.saveReservationDates(reservation, req);

        scheduleDetailService.plusCurrentStudentNumber(req.getReservationDateList(), schedule.getScheduleDetails());

        return reservation;
    }

    public Reservation saveReservation(Account account, Schedule schedule, Payment payment, ReservationCreateReq req) {
        Reservation reservation = Reservation.builder()
                .schedule(schedule)
                .account(account)
                .payment(payment)
                .equipmentList(req.getEquipmentList())
                .description(req.getDescription())
                .build();

        return reservationJpaRepo.save(reservation);
    }
}