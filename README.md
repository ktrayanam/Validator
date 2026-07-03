# PS-15 PAN / Aadhaar Format Validator

This workspace contains a self-contained Java validator for PAN and Aadhaar format checks.

Supported checks:

- PAN: five letters, four digits, one letter, plus the allowed PAN category letter in position 4.
- Aadhaar: 12 ASCII digits plus a Verhoeff checksum validation.
- Normalization: leading and trailing whitespace are trimmed and letters are upper-cased before validation.
- Output: structured pass/fail results with reason codes and human-readable messages.

Notes:

- Internal whitespace, hyphens, and non-ASCII digits are rejected.
- This only validates format and checksum structure. It does not verify that an identity number belongs to a real person.

Run:

```bash
javac IdentityFormatValidator.java
java IdentityFormatValidator ABCPQ1234F
java IdentityFormatValidator 123456789010
```