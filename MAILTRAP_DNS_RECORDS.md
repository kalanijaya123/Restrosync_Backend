# Mailtrap DNS Records for mail.restrosync.com

Add these DNS records at your DNS provider to verify the domain with Mailtrap.

-- CNAME (SMTP)
- Name: `mt91.mail.restrosync.com`
- Type: `CNAME`
- Value/Target: `smtp.mailtrap.live`

-- DKIM CNAMEs
- Name: `rwmt1._domainkey.mail.restrosync.com`
- Type: `CNAME`
- Value/Target: `rwmt1.dkim.smtp.mailtrap.live`

- Name: `rwmt2._domainkey.mail.restrosync.com`
- Type: `CNAME`
- Value/Target: `rwmt2.dkim.smtp.mailtrap.live`

-- Tracking / Links CNAME
- Name: `mt-link.mail.restrosync.com`
- Type: `CNAME`
- Value/Target: `t.mailtrap.live`

-- DMARC TXT (optional / monitoring)
- Name: `_dmarc.mail.restrosync.com`
- Type: `TXT`
- Value: `v=DMARC1; p=none; rua=mailto:postmaster@mail.restrosync.com; ruf=mailto:postmaster@mail.restrosync.com`

Notes:
- Ensure there are no trailing dots or surrounding whitespace in record values.
- TTL can be left as default or set to 3600 seconds.
- Propagation may take from seconds to several hours depending on your DNS provider.

When Mailtrap confirms verification, they will email you. After that, add the SMTP credentials from Mailtrap into the application properties and restart the backend.
