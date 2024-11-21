package com.edu.webapp.service.impl;

import com.edu.webapp.entity.advertisement.AdvertisingPackage;
import com.edu.webapp.entity.advertisement.PayAd;
import com.edu.webapp.entity.user.User;
import com.edu.webapp.error.ErrorCodes;
import com.edu.webapp.error.ValidateException;
import com.edu.webapp.mapper.AdvertisingPackageMapper;
import com.edu.webapp.mapper.PayAdMapper;
import com.edu.webapp.model.enums.ActiveStatus;
import com.edu.webapp.model.request.AdvertisingPackageCreateReq;
import com.edu.webapp.model.request.AdvertisingPackageUpdateReq;
import com.edu.webapp.model.request.PayAdCreateReq;
import com.edu.webapp.model.response.AdvertisingPackageRes;
import com.edu.webapp.model.response.PayAdRes;
import com.edu.webapp.repository.AdvertisingPackageRepository;
import com.edu.webapp.repository.PayAdRepository;
import com.edu.webapp.repository.UserRepository;
import com.edu.webapp.security.JwtCommon;
import com.edu.webapp.service.AdvertisingPackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdvertisingPackageServiceImpl implements AdvertisingPackageService {
    private final AdvertisingPackageRepository advertisingPackageRepository;
    private final AdvertisingPackageMapper advertisingPackageMapper;
    private final JwtCommon jwtCommon;
    private final UserRepository userRepository;
    private final PayAdRepository payAdRepository;
    private final PayAdMapper payAdMapper;

    @Override
    public AdvertisingPackageRes createAdvertisingPackage(AdvertisingPackageCreateReq advertisingPackageCreateReq) {
        AdvertisingPackage advertisingPackage = advertisingPackageMapper.advertisingPackageCreateReqToAdvertisingPackage(advertisingPackageCreateReq);
        String email = jwtCommon.extractUsername();
        advertisingPackage.setCreatedBy(email);
        advertisingPackageRepository.save(advertisingPackage);
        return advertisingPackageMapper.advertisingPackageToAdvertisingPackageRes(advertisingPackage);
    }

    @Override
    public AdvertisingPackageRes getAdvertisingPackage(Integer id) {
        AdvertisingPackage advertisingPackage = advertisingPackageRepository.findById(id)
                .orElseThrow(() -> new ValidateException(ErrorCodes.ADVERTISING_PACKAGE_VALID));
        return advertisingPackageMapper.advertisingPackageToAdvertisingPackageRes(advertisingPackage);
    }

    @Override
    public Page<AdvertisingPackageRes> getAllAdvertisingPackages(Integer page, Integer size, String key) {
        Page<AdvertisingPackage> advertisingPackagePage;
        Pageable pageable;
        if (page == null) {
            pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("createdAt").ascending());
            advertisingPackagePage = advertisingPackageRepository.findAll(pageable);
        } else {
            pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
            advertisingPackagePage = advertisingPackageRepository.findAllByAdvertisingName(key, pageable);
        }
        List<AdvertisingPackageRes> advertisingPackageRes = advertisingPackageMapper.listAdvertisingPackageToListAdvertisingPackageRes(advertisingPackagePage.getContent());
        return new PageImpl<>(advertisingPackageRes, pageable, advertisingPackagePage.getTotalElements());
    }

    @Override
    public AdvertisingPackageRes updateAdvertisingPackage(AdvertisingPackageUpdateReq advertisingPackageUpdateReq) {
        AdvertisingPackage advertisingPackage = advertisingPackageRepository.findById(advertisingPackageUpdateReq.getId())
                .orElseThrow(() -> new ValidateException(ErrorCodes.ADVERTISING_PACKAGE_VALID));
        advertisingPackage.setAdvertisingName(advertisingPackageUpdateReq.getAdvertisingName());
        advertisingPackage.setPrice(advertisingPackageUpdateReq.getPrice());
        advertisingPackage.setDes(advertisingPackageUpdateReq.getDes());
        advertisingPackage.setActive(advertisingPackageUpdateReq.getActive());
        advertisingPackageRepository.save(advertisingPackage);
        return advertisingPackageMapper.advertisingPackageToAdvertisingPackageRes(advertisingPackage);
    }

    @Override
    public PayAdRes createPayAd(PayAdCreateReq payAdCreateReq) {
        AdvertisingPackage advertisingPackage = advertisingPackageRepository.findById(payAdCreateReq.getAdvertisingPackage())
                .orElseThrow(() -> new ValidateException(ErrorCodes.ADVERTISING_PACKAGE_VALID));
        String email = jwtCommon.extractUsername();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ValidateException(ErrorCodes.USER_NOT_EXIST));
        PayAd payAd = new PayAd();
        payAd.setUserId(user.getId());
        payAd.setAdvertisingPackage(advertisingPackage.getId());
        payAd.setPrice(advertisingPackage.getPrice());
        payAdRepository.save(payAd);
        payAdsSuccess(payAd);

        return payAdMapper.payAdToPayAdRes(payAd);
    }

    @Override
    public PayAdRes getPayAd(Integer id) {
        PayAd payAd = payAdRepository.findById(id).orElseThrow(() -> new ValidateException(ErrorCodes.PAY_AD_VALID));
        return payAdMapper.payAdToPayAdRes(payAd);
    }

    @Async
    @Transactional
    public void payAdsSuccess(PayAd payAd) {
        try {
            Thread.sleep(3000);
            payAd.setActive(ActiveStatus.ACTIVE);
            payAdRepository.save(payAd);
            log.info("Pay Ad successful and data = {}", payAd);
            User user = userRepository.findById(payAd.getUserId()).orElseThrow(() -> new ValidateException(ErrorCodes.USER_NOT_EXIST));
            AdvertisingPackage advertisingPackage = advertisingPackageRepository.findById(payAd.getAdvertisingPackage())
                    .orElseThrow(() -> new ValidateException(ErrorCodes.ADVERTISING_PACKAGE_VALID));
            LocalDate rechargeVip = user.getRechargeVip();
            if (rechargeVip != null) {
                rechargeVip.plusDays(advertisingPackage.getCountDate());
            } else rechargeVip = LocalDate.now().plusDays(advertisingPackage.getCountDate());
            user.setRechargeVip(rechargeVip);
            userRepository.save(user);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }
}
