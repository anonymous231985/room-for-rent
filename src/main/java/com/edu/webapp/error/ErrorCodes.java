package com.edu.webapp.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorCodes {
    ROLE_EXIST("ROLE_EXIST", "Vai trò đã tồn tại!"),
    NOT_ROLE_EXIST("NOT_ROLE_EXIST", "NOT_ROLE_EXIST"),
    PERMISSION_NOT_EXIST("PERMISSION_NOT_EXIST", "PERMISSION_NOT_EXIST!"),
    OTP_IS_INCORRECT("OTP_IS_INCORRECT", "OTP_IS_INCORRECT"),
    EMAIL_EXIST("EMAIL_EXIST", "Email đã tồn tại!"),
    POST_NOT_EXIST("POST_NOT_EXIST", "POST_NOT_EXIST"),
    PHONE_EXIST("PHONE_EXIST", "Số điện thoại đã tồn tại!"),
    IMAGE_EMPTY("IMAGE_EMPTY", "IMAGE_EMPTY"),
    UPLOAD_IMAGE_ERROR("UPLOAD_IMAGE_ERROR", "UPLOAD_IMAGE_ERROR"),
    IMAGE_VALID("IMAGE_VALID", "IMAGE_VALID"),
    ADVERTISING_PACKAGE_VALID("ADVERTISING_PACKAGE_VALID", "ADVERTISING_PACKAGE_VALID"),
    PAY_AD_VALID("PAY_AD_VALID", "PAY_AD_VALID"),
    YOU_NOT_PERMISSION_UPDATE("YOU_NOT_PERMISSION_UPDATE", "YOU_NOT_PERMISSION_UPDATE"),
    USER_NOT_EXIST("USER_NOT_EXIST", "Người dùng không tồn tại!"),
    USER_NOT_RECHARGE_VIP("USER_NOT_RECHARGE_VIP", "USER_NOT_RECHARGE_VIP"),
    PASSWORD_OLD_VALID("PASSWORD_OLD_VALID", "PASSWORD_OLD_VALID!"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "An unexpected error occurred");
    private final String code;
    private final String message;
}