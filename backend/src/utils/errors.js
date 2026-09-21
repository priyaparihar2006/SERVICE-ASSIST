export class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}
export function ensure(condition, status, message) {
  if (!condition) throw new HttpError(status, message);
}
export const endpoint = (fn) => async (req, res, next) => {
  try {
    await fn(req, res);
  } catch (error) {
    next(error);
  }
};
