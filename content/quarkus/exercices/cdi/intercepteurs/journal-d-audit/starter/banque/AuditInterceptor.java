package banque;

import jakarta.inject.Inject;
import jakarta.interceptor.InvocationContext;

public class AuditInterceptor {

    @Inject
    Journal journal;

    Object auditer(InvocationContext contexte) throws Exception {
        return contexte.proceed();
    }
}
