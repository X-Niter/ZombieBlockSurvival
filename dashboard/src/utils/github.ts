
export async function fetchChangelog(): Promise<{ version: string, changes: string[] }[]> {
  // TODO: Replace with real GitHub API calls
  return [
    {
      version: "v1.0.1",
      changes: ["Added AI workflow runner", "Refactored UI into modular layout", "Linked OpenAI simulation"]
    },
    {
      version: "v1.0.0",
      changes: ["Initial UI dashboard release"]
    }
  ];
}
