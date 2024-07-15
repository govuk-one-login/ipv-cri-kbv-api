import { LambdaInterface } from "@aws-lambda-powertools/commons";

export class TimeFunction implements LambdaInterface {
  public async handler(_event: unknown, _context: unknown): Promise<any> {
    return Date.now();
  }
}

const handlerClass = new TimeFunction();
export const lambdaHandler = handlerClass.handler.bind(handlerClass);
