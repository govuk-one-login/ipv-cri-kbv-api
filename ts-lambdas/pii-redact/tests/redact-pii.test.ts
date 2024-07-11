import { redactPII } from "../src/pii-redactor";

describe("redact-pii", () => {
  describe("Personal Numbers", () => {
    it("should redact nino field", async () => {
      const ninoRedacted = '{\\"nino\\": \\"***\\"}';
      const nino = '{\\"nino\\": \\"AA000003D\\"}';
      expect(redactPII(nino)).toStrictEqual(ninoRedacted);
    });

    it("should redact personalNumber field", async () => {
      const personalNumberRedacted = '{\\"personalNumber\\": \\"***\\"}';
      const personalNumber = '{\\"personalNumber\\":\\"123\\"}';
      expect(redactPII(personalNumber)).toStrictEqual(personalNumberRedacted);
    });
  });

  describe("IP Addresses", () => {
    it("should redact ip_address field", async () => {
      const ipRedacted = '{\\"ip_address\\": \\"***\\"}';
      const ipSingle = '{\\"ip_address\\":\\"localhost\\"}';
      expect(redactPII(ipSingle)).toStrictEqual(ipRedacted);
      const ipMulti = '{\\"ip_address\\":\\"localhost, localhost\\"}';
      expect(redactPII(ipMulti)).toStrictEqual(ipRedacted);
    });
  });

  describe("Names", () => {
    it("should redact firstName field", async () => {
      const firstNameRedacted = '{\\"firstName\\": \\"***\\"}';
      const firstName = '{\\"firstName\\":\\"Joe\\"}';
      expect(redactPII(firstName)).toStrictEqual(firstNameRedacted);
    });

    it("should redact lastName field", async () => {
      const lastNameRedacted = '{\\"lastName\\": \\"***\\"}';
      const lastName = '{\\"lastName\\":\\"Bloggs\\"}';
      expect(redactPII(lastName)).toStrictEqual(lastNameRedacted);
    });

    it("should redact GivenName and FamilyName fields", async () => {
      const familyNameRedacted =
        '\\"type\\": \\"FamilyName\\", \\"value\\": \\"***\\"';
      const familyName = '\\"type\\":\\"FamilyName\\",\\"value\\":\\"test\\"';
      expect(redactPII(familyName)).toStrictEqual(familyNameRedacted);

      const givenNameRedacted =
        '\\"type\\": \\"GivenName\\", \\"value\\": \\"***\\"';
      const givenName = '\\"type\\":\\"GivenName\\",\\"value\\":\\"test\\"';
      expect(redactPII(givenName)).toStrictEqual(givenNameRedacted);
    });
  });

  describe("Dates of Birth", () => {
    it("should redact birthDates field", async () => {
      const birthDatesRedacted = '{\\"birthDates\\": \\"***\\"}';
      const birthDates =
        '{\\"birthDates\\":{\\"L\\":[{\\"M\\":{\\"value\\":{\\"S\\":\\"1970-01-01\\"}}}]}}';
      expect(redactPII(birthDates)).toStrictEqual(birthDatesRedacted);
    });

    it("should redact birthDate field", async () => {
      const birthDateRedacted = '\\"birthDate\\":[{\\"value\\":\\"***\\"}]';
      const birthDate = '\\"birthDate\\":[{\\"value\\":\\"1970-01-01\\"}]';
      expect(redactPII(birthDate)).toStrictEqual(birthDateRedacted);
    });
  });

  describe("Others", () => {
    it("should redact userId field", async () => {
      const userIdRedacted = '{\\"user_id\\": \\"***\\"}';
      const userId =
        '{\\"user_id\\":\\"urn:fdc:gov.uk:2022:6fe97b75-05a3-43e1-aa82-bb6140eaee3a\\"}';
      expect(redactPII(userId)).toStrictEqual(userIdRedacted);
    });

    it("should redact subject field", async () => {
      const subjectRedacted = '{\\"subject\\": \\"***\\"}';
      const subject = '{\\"subject\\":\\"subject\\"}';
      expect(redactPII(subject)).toStrictEqual(subjectRedacted);
    });

    it("should redact token field", async () => {
      const tokenRedacted = '{\\"token\\": \\"***\\"}';
      const token = '{\\"token\\":\\"goodToken\\"}';
      expect(redactPII(token)).toStrictEqual(tokenRedacted);
    });
  });

  describe("Addresses", () => {
    it("should redact buildingName field", async () => {
      const buildingNameRedacted =
        '{\\"buildingName\\": { \\"S\\": \\"***\\"}}';
      const buildingName = '{\\"buildingName\\": {\\"S\\":\\"GDS\\"}}';
      expect(redactPII(buildingName)).toStrictEqual(buildingNameRedacted);
    });

    it("should redact addressLocality field", async () => {
      const addressLocalityRedacted =
        '{\\"addressLocality\\": { \\"S\\": \\"***\\"}}';
      const addressLocality = '{\\"addressLocality\\": {\\"S\\":\\"Test\\"}}';
      expect(redactPII(addressLocality)).toStrictEqual(addressLocalityRedacted);
    });

    it("should redact buildingNumber field", async () => {
      const buildingNumberRedacted =
        '{\\"buildingNumber\\": { \\"S\\": \\"***\\"}}';
      const buildingNumber = '{\\"buildingNumber\\": {\\"S\\":\\"1\\"}}';
      expect(redactPII(buildingNumber)).toStrictEqual(buildingNumberRedacted);
    });

    it("should redact postalCode field", async () => {
      const postalCodeRedacted = '{\\"postalCode\\": { \\"S\\": \\"***\\"}}';
      const postalCode = '{\\"postalCode\\": {\\"S\\":\\"LU3\\"}}';
      expect(redactPII(postalCode)).toStrictEqual(postalCodeRedacted);
    });

    it("should redact streetName field", async () => {
      const streetNameRedacted = '{\\"streetName\\": { \\"S\\": \\"***\\"}}';
      const streetName = '{\\"streetName\\": {\\"S\\":\\"whitechapel\\"}}';
      expect(redactPII(streetName)).toStrictEqual(streetNameRedacted);
    });
  });

  describe("Non-PII fields", () => {
    it("should not redact govuk_signin_journey_id field", async () => {
      const govSigninJourneyID =
        '{\\"userAuditInfo\\":{\\"govuk_signin_journey_id\\":\\"02d56902-96bb-4669-ba18-be6c2b002f36\\"}';
      expect(redactPII(govSigninJourneyID)).toStrictEqual(govSigninJourneyID);
    });

    it("should not redact sessionCheck field", async () => {
      const sessionCheck =
        '{"\\sessionCheck\\":{\\"status\\":\\"SESSION_OK\\",\\"clientId\\":\\"ipv-core-stub-aws-prod\\",\\"userAuditInfo\\":{}}}';
      expect(redactPII(sessionCheck)).toStrictEqual(sessionCheck);
    });

    it("should not redact date fields", async () => {
      const dateWithBrackets = '"Date":["Sat, 11 May 2024 11:24:11 GMT"]';
      expect(redactPII(dateWithBrackets)).toStrictEqual(dateWithBrackets);
      const dateWithoutBrackets = '"Date":"Sat, 11 May 2024 11:24:24 GMT",';
      expect(redactPII(dateWithoutBrackets)).toStrictEqual(dateWithoutBrackets);
    });
  });
});
