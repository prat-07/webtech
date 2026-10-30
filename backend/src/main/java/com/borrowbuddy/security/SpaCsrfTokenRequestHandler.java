package com.borrowbuddy.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.*;
import org.springframework.util.StringUtils;

/** Uses masked tokens for rendered forms and the raw cookie token for SPA request headers. */
public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
  private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
  private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> deferredToken) {
    xor.handle(request, response, deferredToken);
    deferredToken.get();
  }

  @Override
  public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken token) {
    String header = request.getHeader(token.getHeaderName());
    return StringUtils.hasText(header)
        ? plain.resolveCsrfTokenValue(request, token)
        : xor.resolveCsrfTokenValue(request, token);
  }
}
