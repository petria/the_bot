# Repository Workflow

## Feature Branch Promotion

- Start all new feature work on the `develop` branch. Do not implement new features directly on `main`.
- Before editing, verify that the current branch is `develop`. Preserve unrelated worktree changes when switching branches.
- Implement and test the feature on `develop`, then commit it and push `develop` to `origin` so the develop CI pipeline runs.
- Do not merge a feature to `main` until the user explicitly confirms that the feature has been accepted as working.
- After that explicit acceptance, merge `develop` into `main` and push `main` to `origin` so the main CI pipeline runs.
- A successful develop CI run alone is not approval to promote the feature to `main`.

## Frontend Testing

- New or changed React pages, components, and UI behavior must include or update unit/component tests in the same change.
- Write the relevant UI test before implementation where practical, then keep iterating until the test passes.
- Run `npm test` and `npm run build` in `the_bot_web/frontend` before accepting the UI work.
- Frontend tests are part of `npm run build`; a test failure must fail the build and block acceptance.
- Regression fixes must include a test that reproduces the original failure.
