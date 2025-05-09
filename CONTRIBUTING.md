# Contributing to SevenToDie Plugin

Thank you for your interest in contributing to the SevenToDie Minecraft plugin! This document provides guidelines and instructions for contributing to this project.

## Code of Conduct

By participating in this project, you agree to maintain a respectful and inclusive environment for everyone. We expect all contributors to:

- Be respectful and considerate in all interactions
- Focus on what is best for the community
- Show empathy towards other community members

## How Can I Contribute?

### Reporting Bugs

If you've found a bug, please create an issue using the bug report template. Include as much detail as possible:

- A clear and descriptive title
- Steps to reproduce the behavior
- Expected behavior
- Current behavior
- Screenshots if applicable
- Server environment details

### Suggesting Features

We welcome feature suggestions! Please use the feature request template when creating an issue and provide:

- A clear description of the feature
- How it would enhance the plugin
- If it's based on a feature from the original 7 Days To Die game, include details on how it works there

### Pull Requests

1. Fork the repository
2. Create a feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Submit a pull request

When submitting a pull request:

- Provide a clear description of the changes
- Link any related issues
- Update documentation if necessary
- Ensure your code compiles and works as expected
- Write unit tests if possible

## Development Setup

Please refer to the [DEVELOPMENT.md](DEVELOPMENT.md) file for detailed instructions on setting up your development environment.

## Style Guidelines

### Code Style

This project follows standard Java code style conventions:

- Use 4 spaces for indentation
- Follow Java naming conventions
  - `camelCase` for methods and variables
  - `PascalCase` for classes
  - `UPPER_SNAKE_CASE` for constants
- Add JavaDoc comments to public classes and methods
- Keep methods focused on a single responsibility
- Limit line length to 100 characters where reasonable

### Commit Messages

- Use clear, descriptive commit messages
- Start with a short summary line, followed by details if needed
- Reference issue numbers when applicable (`Fixes #123`)
- Use present tense ("Add feature" not "Added feature")

## Project Structure

Refer to the project structure section in [DEVELOPMENT.md](DEVELOPMENT.md) for a detailed overview of the codebase organization.

## Testing

- Test your changes in a clean Minecraft server environment
- Verify that your changes don't break existing functionality
- For new features, ensure they work as expected in various scenarios

## Documentation

- Update README.md if you add, remove, or change functionality
- Add JavaDoc comments to new public classes and methods
- If your change requires users to modify their configuration, document this

## Questions?

If you have any questions about contributing, feel free to open an issue with your question.

Thank you for contributing to the SevenToDie plugin!