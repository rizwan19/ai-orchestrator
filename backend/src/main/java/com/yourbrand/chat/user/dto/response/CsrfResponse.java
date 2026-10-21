package com.yourbrand.chat.user.dto.response;

/** Tells the frontend which header must echo the XSRF-TOKEN cookie. */
public record CsrfResponse(String headerName) {}
