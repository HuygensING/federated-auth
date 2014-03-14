federated-auth
==============

## What is 'federated-auth'?
Federated-auth is the Huygens ING authentication and authorisation layer. It
acts as an authorization interface between front-end and back-end layers of
various Huygens ING projects, e.g., Elaborate and implements federated
authentication by communicating with SAML2 enabled Identity Providers (IdP), such as SURFconext.

We are currently in the process of adding the CLARIN IdP to our pool of supported Identity Providers.

## How does it work?
Federated-auth is a Java REST-service, based on JAX-RS, implemented using Jersey. It relies on OpenSAML for SAML messages related to login and assertion consumption.

## Supported REST calls

## Test page (browser)
The [Federated Auth Test](https://secure.huygens.knaw.nl/static/index.html) page demonstrates a sample service, which shows information about the authenticated user as obtained through SAML negotiation from the Identity
Provider (IdP) which was used to authenticate.

The service can be test by anyone with an account at any of the institutes
connected via SURFconext. After loading, the page will redirect via SURF IdP to the IdP of any selected institute. Upon providing valid credentials to this IdP, the user's browser will be redirected to the test page, to present the data about the authenticated user in the browser.
