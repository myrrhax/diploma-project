export default interface ErrorResponse {
    message: string,
    errors: Map<string, string[]> | null
}