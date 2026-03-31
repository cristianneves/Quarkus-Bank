package br.com.bb.transacoes.service;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Serviço responsável pela reserva atômica da chave de idempotência via
 * {@code INSERT ... ON CONFLICT DO NOTHING}.
 *
 * <p>Extraído de {@link TransferenciaService} para permitir que seja
 * substituído por um mock ({@code @InjectMock}) nos testes de unidade do
 * serviço sem depender de um {@code EntityManager} real ou de
 * {@code PanacheMock}.</p>
 *
 * <p><b>Garantia de atomicidade:</b> O PostgreSQL executa o INSERT como uma
 * operação atômica. Dois threads concorrentes com a mesma chave nunca passam
 * desta etapa simultaneamente: o segundo bloqueia até o primeiro commitar/
 * rollbackar (READ COMMITTED), resolvendo o conflito de forma determinística
 * sem {@code ConstraintViolationException}.</p>
 */
@ApplicationScoped
public class IdempotencyClaimService {

    @Inject
    EntityManager em;

    /**
     * Tenta reservar atomicamente a chave de idempotência.
     *
     * <p>Deve ser chamado dentro de uma transação ativa
     * ({@link Transactional.TxType#MANDATORY}).</p>
     *
     * @param idempotencyKey chave única da operação (UUID do cliente)
     * @return {@code true}  → chave reservada com sucesso; nova operação pode prosseguir.
     *         {@code false} → chave já existia; operação duplicada, retornar idempotente.
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public boolean tryClaimKey(String idempotencyKey) {
        int rows = em.createNativeQuery("""
                        INSERT INTO transferencia (id, idempotencyKey, status, dataHora)
                        VALUES (nextval('Transferencia_SEQ'), :key, 'PROCESSANDO', now())
                        ON CONFLICT (idempotencyKey) DO NOTHING
                        """)
                .setParameter("key", idempotencyKey)
                .executeUpdate();

        if (rows == 0) {
            Log.warnf("⚠️ Chave idempotente já processada: [%s]. Retornando resultado existente.", idempotencyKey);
        }
        return rows > 0;
    }
}

