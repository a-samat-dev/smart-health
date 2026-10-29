package kz.samat.smarthealth.userservice.model.entity;

public enum UserType {

    /** Books appointments with doctors. Every user starts as a client. */
    CLIENT,

    /** Registered with sh-doctor-service, with a license and specializations, and sees patients. */
    DOCTOR
}
