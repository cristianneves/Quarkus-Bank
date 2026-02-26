package br.com.bb.transacoes.interceptor;

import io.quarkus.logging.Log;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.eclipse.microprofile.jwt.JsonWebToken;

@AuditAdmin
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class AdminAuditInterceptor {

    @Inject
    JsonWebToken jwt;

    @AroundInvoke
    public Object audit(InvocationContext context) throws Exception {
        // 1. Antes de executar o método: Pegamos quem está logado
        String adminEmail = jwt.getClaim("email");
        String adminId = jwt.getSubject();
        String methodName = context.getMethod().getName();
        Object[] parameters = context.getParameters();

        // 2. Log de Início (Traceability)
        Log.warnf("⚠️ AUDITORIA: Admin [%s - %s] iniciou operação: %s com parâmetros: %s",
                adminEmail, adminId, methodName, parameters[0]);

        try {
            // 3. Executa o método real (o depósito)
            Object result = context.proceed();

            // 4. Log de Sucesso
            Log.infof("✅ AUDITORIA: Operação %s concluída com sucesso pelo Admin %s",
                    methodName, adminEmail);

            return result;
        } catch (Exception e) {
            // 5. Log de Falha (Vital para segurança)
            Log.errorf("🚨 AUDITORIA: Falha na operação %s pelo Admin %s. Motivo: %s",
                    methodName, adminEmail, e.getMessage());
            throw e;
        }
    }
}