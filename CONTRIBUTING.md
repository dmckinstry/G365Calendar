# Contributing to G365Calendar

Thank you for your interest in contributing to G365Calendar! This document provides guidelines for contributing to the project.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/G365Calendar.git`
3. Open the project in VS Code with Dev Containers extension
4. VS Code will prompt to reopen in container - accept to get the full development environment

## Development Workflow

### 1. Create a Feature Branch

```bash
git checkout -b feature/your-feature-name
```

### 2. Make Your Changes

- Follow the existing code style
- Write clear, commented code where necessary
- Test your changes on actual devices when possible

### 3. Test Your Changes

- Build the watch app: `monkeyc -d venu2 -f monkey.jungle -o bin/G365Calendar.prg -y .keys/developer_key.der`
- Test in the Connect IQ simulator
- If possible, test on an actual Garmin device

### 4. Commit Your Changes

```bash
git add .
git commit -m "feat: add your feature description"
```

Use conventional commit messages:
- `feat:` for new features
- `fix:` for bug fixes
- `docs:` for documentation changes
- `refactor:` for code refactoring
- `test:` for adding tests
- `chore:` for maintenance tasks

### 5. Push and Create Pull Request

```bash
git push origin feature/your-feature-name
```

Then create a pull request on GitHub.

## Code Style Guidelines

### Monkey C (Watch App)

- Use descriptive variable and function names
- Follow camelCase for variables and functions
- Use PascalCase for classes
- Add comments for complex logic
- Keep functions focused and single-purpose

Example:
```monkey-c
function formatDateTime(dateInfo as Gregorian.Info) as String {
    // Format as ISO 8601: YYYY-MM-DDTHH:MM:SSZ
    return dateInfo.year.format("%04d") + "-" +
           dateInfo.month.format("%02d") + "-" +
           dateInfo.day.format("%02d") + "T" +
           dateInfo.hour.format("%02d") + ":" +
           dateInfo.min.format("%02d") + ":" +
           dateInfo.sec.format("%02d") + "Z";
}
```

### Java (Android App)

- Follow standard Java conventions
- Use Android best practices
- Handle errors gracefully
- Add null checks where appropriate

### Swift (iOS App)

- Follow Swift API Design Guidelines
- Use meaningful variable names
- Leverage Swift's type system
- Handle optionals safely

## Security Considerations

⚠️ **NEVER commit:**
- OAuth client secrets
- API keys
- Developer keys (.pem, .der files)
- Access tokens
- Personal authentication credentials

These should be:
- Stored in `.keys/` directory (gitignored)
- Configured via environment variables
- Documented in setup instructions

## Testing Guidelines

### Watch App Testing

1. Test in Connect IQ Simulator for basic functionality
2. Test on actual device for:
   - OAuth flows
   - Network requests
   - UI rendering
   - Touch interactions
   - Battery impact

### Companion App Testing

1. Test on multiple Android versions (if possible)
2. Test OAuth authentication flow
3. Test device communication
4. Verify data synchronization

## Documentation

When adding features:

1. Update README.md if it affects usage
2. Update inline code comments
3. Add/update API documentation
4. Update configuration examples

## Pull Request Process

1. **Ensure CI passes**: All automated checks must pass
2. **Update documentation**: Include relevant documentation updates
3. **Describe changes**: Provide clear description of what changed and why
4. **Reference issues**: Link to related issues (e.g., "Fixes #123")
5. **Screenshots**: For UI changes, include before/after screenshots
6. **Testing notes**: Describe how you tested the changes

### PR Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Documentation update
- [ ] Performance improvement

## Testing
How were these changes tested?

## Screenshots (if applicable)

## Related Issues
Fixes #(issue number)
```

## Areas for Contribution

### High Priority

- [ ] Additional device support (fenix, forerunner series)
- [ ] Improved error handling and user feedback
- [ ] Battery optimization
- [ ] Event filtering and search

### Medium Priority

- [ ] Unit tests for Monkey C code
- [ ] UI/UX improvements
- [ ] Additional calendar providers
- [ ] Event reminders/notifications

### Documentation

- [ ] Video tutorials
- [ ] Troubleshooting guide
- [ ] FAQ section
- [ ] Localization (i18n)

## Questions or Problems?

- Open an issue for bugs or feature requests
- Use discussions for questions and general topics
- Check existing issues before creating new ones

## Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Help others learn and grow
- Keep discussions focused and professional

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

Thank you for contributing to G365Calendar! 🎉
