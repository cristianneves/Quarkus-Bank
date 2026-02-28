package br.com.bb.transacoes.interceptor;

import br.com.bb.transacoes.model.Auditoria;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.Arrays;

@AuditAdmin
@Interceptor
@Priority(jakarta.interceptor.Interceptor.Priority.APPLICATION)
public class AdminAuditInterceptor {

    @Inject
    SecurityIdentity identity;

    @AroundInvoke
    public Object audit(InvocationContext context) throws Exception {
        String adminName = identity.getPrincipal().getName();
        String methodName = context.getMethod().getName();
        String parameters = Arrays.toString(context.getParameters());

        try {
            // 1. Executa a operação real (o depósito no service)
            Object result = context.proceed();

            // 2. Grava o log de SUCESSO em uma NOVA transação isolada
            QuarkusTransaction.requiringNew().run(() -> {
                new Auditoria(adminName, methodName, "SUCESSO: " + parameters).persist();
            });

            Log.infof("✅ Auditoria persistida para: %s", adminName);
            return result;

        } catch (Exception e) {
            // 3. Grava o log de ERRO também em uma nova transação
            QuarkusTransaction.requiringNew().run(() -> {
                new Auditoria(adminName, methodName, "FALHA: " + e.getMessage()).persist();
            });

            Log.errorf("🚨 Auditoria de erro persistida para: %s", adminName);
            throw e;
        }
    }
}