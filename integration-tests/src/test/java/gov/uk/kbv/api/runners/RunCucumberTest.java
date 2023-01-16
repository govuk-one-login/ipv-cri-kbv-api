package gov.uk.kbv.api.runners;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        plugin = {
            "json:target/cucumber.json",
            "html:target/default-html-reports",
            "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
        },
        features = "src/test/resources",
        glue = "gov/uk/kbv/api/stepDefinitions",
        dryRun = false)
public class RunCucumberTest {}
