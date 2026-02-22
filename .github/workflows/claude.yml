name: Claude Code

on:
  issue_comment:
    types: [created]
  pull_request_review_comment:
    types: [created]
  issues:
    types: [opened, assigned]
  pull_request_review:
    types: [submitted]

jobs:
  claude:
    if: |
      (github.event_name == 'issue_comment' && contains(github.event.comment.body, '@claude')) ||
      (github.event_name == 'pull_request_review_comment' && contains(github.event.comment.body, '@claude')) ||
      (github.event_name == 'pull_request_review' && contains(github.event.review.body, '@claude')) ||
      (github.event_name == 'issues' && (contains(github.event.issue.body, '@claude') || contains(github.event.issue.title, '@claude')))
    runs-on: ubuntu-latest
    permissions:
      contents: write
      pull-requests: write
      issues: write
      id-token: write
      actions: read # Required for Claude to read CI results on PRs
    steps:
      - name: Checkout repository
        uses: actions/checkout@v6
        with:
          fetch-depth: 1

      - name: Run Claude Code
        id: claude
        uses: anthropics/claude-code-action@v1
        with:
          anthropic_api_key: ${{ secrets.ANTHROPIC_API_KEY }}

          # Optional: Customize the trigger phrase (default: @claude)
          # trigger_phrase: "/claude"

          # Optional: Trigger when specific user is assigned to an issue
          # assignee_trigger: "claude-bot"

          # Optional: Configure Claude's behavior with CLI arguments
          # claude_args: |
          #   --model claude-opus-4-1-20250805
          #   --max-turns 10
          #   --allowedTools "Bash(npm install),Bash(npm run build),Bash(npm run test:*),Bash(npm run lint:*)"
          #   --system-prompt "Follow our coding standards. Ensure all new code has tests. Use TypeScript for new files."

          # Optional: Advanced settings configuration
          # settings: |
          #   {
          #     "env": {
          #       "NODE_ENV": "test"
          #     }
          #   }
