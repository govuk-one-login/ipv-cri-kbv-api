package uk.gov.di.ipv.cri.kbv.acceptancetest.journey;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KBVJourneyTest {

    @DisplayName("answer all questions correctly and produce a VC with a verification score of 2")
    @Test
    void shouldPassKBV() throws Exception {
        HtmlPage page1OfQuestions = loadKBVPage();
        HtmlPage page2OfQuestions = chooseAnswerForQuestion1(page1OfQuestions, 0);
        HtmlPage vcResultPage = chooseAnswerForQuestion2(page2OfQuestions, 0);
        checkVerifiableCredential(vcResultPage);
    }

    @DisplayName(
            "do not answer minimum questions correctly and produce a VC with a verification score of 0")
    @Test
    void shouldNotPassKBV() throws Exception {
        HtmlPage page1OfQuestions = loadKBVPage();
        HtmlPage page2OfQuestions = chooseAnswerForQuestion1(page1OfQuestions, 0);
        HtmlPage vcResultPage = chooseAnswerForQuestion2(page2OfQuestions, 1);
        checkVerifiableCredential(vcResultPage);
    }

    private HtmlPage chooseAnswerForQuestion1(HtmlPage page, int answerIndex) throws IOException {
        assertEquals("Question 1 – Prove your identity – GOV.UK", page.getTitleText());
        String pageAsText = page.asNormalizedText();
        assertTrue(pageAsText.contains("Question 1"));
        assertTrue(pageAsText.contains("Correct 1"));
        assertTrue(pageAsText.contains("Incorrect 1"));
        HtmlForm form = selectAnswer(page, "Q00001", answerIndex);
        return clickContinueButton(form);
    }

    private HtmlPage chooseAnswerForQuestion2(HtmlPage page, int answerIndex) throws IOException {
        assertEquals("Question 2 – Prove your identity – GOV.UK", page.getTitleText());
        final String pageAsText = page.asNormalizedText();
        assertTrue(pageAsText.contains("Question 2"));
        assertTrue(pageAsText.contains("Correct 2"));
        assertTrue(pageAsText.contains("Incorrect 2"));
        HtmlForm form = selectAnswer(page, "Q00002", answerIndex);
        return clickContinueButton(form);
    }

    private HtmlForm selectAnswer(
            HtmlPage page, String questionId, int answerIndex) {
        List<HtmlForm> forms = page.getForms();
        assertNotNull(forms, "no forms on page");
        assertEquals(1, forms.size(), "unexpected number of forms on page");
        HtmlForm form = forms.get(0);
        List<HtmlRadioButtonInput> answers = form.getRadioButtonsByName(questionId);
        HtmlRadioButtonInput question = answers.get(answerIndex);
        question.setChecked(true);
        return form;
    }

    private HtmlPage clickContinueButton(HtmlForm form) throws IOException {
        HtmlElement e = findContinueButton(form);
        assertTrue(e instanceof HtmlButton);
        HtmlButton button = (HtmlButton) e;
        Page nextPage = button.click();
        assertTrue(nextPage instanceof HtmlPage);
        return (HtmlPage) nextPage;
    }

    private HtmlElement findContinueButton(HtmlForm form) {
        return form.getOneHtmlElementByAttribute("button", "class", "govuk-button button");
    }

    private void checkVerifiableCredential(HtmlPage vcResultPage) {
        final String pageAsText = vcResultPage.asNormalizedText();
        assertTrue(pageAsText.contains("Verifiable Credentials"));
    }

    private HtmlPage loadKBVPage() throws IOException {
        WebClient webClient = new WebClient();
        setCredentials(webClient);
        String startURL = getEnvVar("IPV_CORE_STUB_START_URL");
        return webClient.getPage(startURL);
    }

    private void setCredentials(WebClient webClient) {
        webClient.addRequestHeader(
                "Authorization",
                "Basic "
                        + Base64.getEncoder()
                                .encodeToString(
                                        (getEnvVar("IPV_CORE_STUB_AUTH_USERNAME")
                                                        + ":"
                                                        + getEnvVar("IPV_CORE_STUB_AUTH_PASSWORD"))
                                                .getBytes(StandardCharsets.UTF_8)));
    }

    private String getEnvVar(String envVar) {
        return Optional.ofNullable(System.getenv(envVar))
                .orElseThrow(() -> new IllegalStateException(envVar + "env var not present"));
    }
}
