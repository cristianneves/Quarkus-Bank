package br.com.bb.cadastro.unit.exception;

import br.com.bb.cadastro.exception.GlobalExceptionMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionMapperTest {

    private GlobalExceptionMapper mapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        mapper = new GlobalExceptionMapper();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("Deve mapear WebApplicationException para Response com status e mensagem")
    void deveMapearWebApplicationException() {
        WebApplicationException exception = new WebApplicationException("Usuário não encontrado", 404);
        
        Response response = mapper.toResponse(exception);
        
        assertEquals(404, response.getStatus());
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertEquals(404, entity.get("status"));
        assertEquals("Usuário não encontrado", entity.get("erro"));
    }

    @Test
    @DisplayName("Deve mapear ConstraintViolationException de validação para 400 Bad Request")
    void deveMapearValidationConstraintViolationException() {
        TestEntity entity = new TestEntity();
        entity.value = null;
        
        Set<ConstraintViolation<TestEntity>> violations = validator.validate(entity);
        ConstraintViolationException violationException = new ConstraintViolationException(violations);
        
        Response response = mapper.toResponse(violationException);
        
        assertEquals(400, response.getStatus());
        Map<String, Object> entityMap = (Map<String, Object>) response.getEntity();
        assertEquals("Validação falhou", entityMap.get("erro"));
    }

    @Test
    @DisplayName("Deve mapear ConstraintViolationException (causa raiz) para 409 Conflict")
    void deveMapearConstraintViolationException() {
        org.hibernate.exception.ConstraintViolationException cause = 
            new org.hibernate.exception.ConstraintViolationException("Duplicate key", null, "uq_cpf");
        RuntimeException exception = new RuntimeException(cause);
        
        Response response = mapper.toResponse(exception);
        
        assertEquals(409, response.getStatus());
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertEquals("Conflito de dados", entity.get("erro"));
    }

    @Test
    @DisplayName("Deve mapear exceção genérica para 500 Internal Server Error")
    void deveMapearExcecaoGenericaPara500() {
        RuntimeException exception = new RuntimeException("Algo falhou");
        
        Response response = mapper.toResponse(exception);
        
        assertEquals(500, response.getStatus());
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertEquals("Erro interno no servidor", entity.get("erro"));
        assertEquals("Algo falhou", entity.get("msg"));
    }

    @Test
    @DisplayName("Deve encontrar ConstraintViolationException em causa aninhada")
    void deveEncontrarCausaAninhada() {
        org.hibernate.exception.ConstraintViolationException constraintEx = 
            new org.hibernate.exception.ConstraintViolationException("Violation", null, "uq_email");
        RuntimeException exception = new RuntimeException(new RuntimeException("Intermediária", constraintEx));
        
        Response response = mapper.toResponse(exception);
        
        assertEquals(409, response.getStatus());
    }

    @Test
    @DisplayName("Deve mapear erro não mapeado para 500")
    void deveMapearOutroTipoDeErro() {
        IllegalArgumentException exception = new IllegalArgumentException("Argumento inválido");
        
        Response response = mapper.toResponse(exception);
        
        assertEquals(500, response.getStatus());
    }

    private static class TestEntity {
        @NotBlank(message = "não pode estar em branco")
        String value;
    }
}