import type { Config } from "jest";
import baseConfig from "../../jest.config.base";

export default {
  ...baseConfig,
  displayName: "ts-lambdas/pii-redact",
} satisfies Config;
