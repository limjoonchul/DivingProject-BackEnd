package com.diving.pungdong.controller;

import com.diving.pungdong.advice.exception.CAuthenticationEntryPointException;
import com.diving.pungdong.advice.exception.ForbiddenTokenException;
import com.diving.pungdong.model.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/exception")
public class ExceptionController {

    @GetMapping(value = "/entrypoint")
    public CommonResult entrypointException() {
        throw new CAuthenticationEntryPointException();
    }

    @GetMapping(value = "/accessDenied")
    public CommonResult accessDeniedException() {
        throw new AccessDeniedException("");
    }

    @GetMapping(value = "/forbiddenToken")
    public void forbiddenTokenException() {
        throw new ForbiddenTokenException();
    }
}
