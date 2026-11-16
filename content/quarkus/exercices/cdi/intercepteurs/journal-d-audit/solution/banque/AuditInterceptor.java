package banque;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Audite
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class AuditInterceptor {

    @Inject
    Journal journal;

    @AroundInvoke
    Object auditer(InvocationContext contexte) throws Exception {
        String methode = contexte.getMethod().getName();
        try {
            Object resultat = contexte.proceed();
            journal.ecrire(methode + " : ok");
            return resultat;
        } catch (Exception e) {
            journal.ecrire(methode + " : échec");
            throw e;
        }
    }
}
