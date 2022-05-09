package uk.gov.di.ipv.cri.experian.kbv.api.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.experian.kbv.api.domain.PersonIdentity;
import uk.gov.di.ipv.cri.experian.kbv.api.domain.ValidationResult;
import uk.gov.di.ipv.cri.experian.kbv.api.util.TestDataCreator;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InputValidationExecutorTest {
    @Mock private Validator mockValidator;
    private InputValidationExecutor inputValidationExecutor;

    @BeforeEach
    void setUp() {
        inputValidationExecutor = new InputValidationExecutor(mockValidator);
    }

    @Test
    void shouldReturnValidValidationResultWhenValidInputProvided() {
        PersonIdentity personIdentity = TestDataCreator.createTestPersonIdentity();
        when(mockValidator.validate(personIdentity)).thenReturn(new HashSet<>());

        ValidationResult validationResult =
                inputValidationExecutor.performInputValidation(personIdentity);

        assertTrue(validationResult.isValid());
        assertEquals(0, validationResult.getErrors().size());
    }

    @Test
    void shouldReturnInvalidValidationResultWhenInvalidInputProvided() {
        final String validationErrorMsg = "validation error message";
        final PersonIdentity personIdentity = TestDataCreator.createTestPersonIdentity();

        ConstraintViolation<?> mockConstraintViolation = Mockito.mock(ConstraintViolation.class);
        when(mockConstraintViolation.getMessage()).thenReturn(validationErrorMsg);
        Mockito.doReturn(Set.of(mockConstraintViolation))
                .when(mockValidator)
                .validate(personIdentity);

        ValidationResult validationResult =
                inputValidationExecutor.performInputValidation(personIdentity);

        assertFalse(validationResult.isValid());
        assertEquals(1, validationResult.getErrors().size());
        assertEquals(validationErrorMsg, validationResult.getErrors().get(0));
    }
}
