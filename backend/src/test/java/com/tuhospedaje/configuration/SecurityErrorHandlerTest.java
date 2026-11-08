package com.tuhospedaje.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Locale;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SecurityErrorHandlerTest {

    @Test
    @DisplayName("Does not write or mutate response when response is already committed on handle()")
    void doesNotWriteWhenResponseAlreadyCommittedOnHandle() throws Exception {
        MessageSource messageSource = mock(MessageSource.class);
        JsonMapper jsonMapper = JsonMapper.builder().build();
        SecurityErrorHandler handler = new SecurityErrorHandler(messageSource, jsonMapper);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.isCommitted()).thenReturn(true);

        handler.handle(request, response, new AccessDeniedException("Denied"));

        verify(response, never()).setStatus(HttpStatus.FORBIDDEN.value());
        verify(response, never()).getWriter();
        verifyNoInteractions(messageSource);
    }

    @Test
    @DisplayName("Does not write or mutate response when response is already committed on entryPoint()")
    void doesNotWriteWhenResponseAlreadyCommittedOnEntryPoint() throws Exception {
        MessageSource messageSource = mock(MessageSource.class);
        JsonMapper jsonMapper = JsonMapper.builder().build();
        SecurityErrorHandler handler = new SecurityErrorHandler(messageSource, jsonMapper);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getLocale()).thenReturn(Locale.ENGLISH);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.isCommitted()).thenReturn(true);

        handler.entryPoint(HttpStatus.UNAUTHORIZED).commence(request, response, null);

        verify(response, never()).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(response, never()).getWriter();
        verifyNoInteractions(messageSource);
    }
}
