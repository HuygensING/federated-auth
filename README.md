federated-auth
==============

## What is 'federated-auth'?
Federated-auth is the Huygens ING authentication and authorisation layer. It
acts as an authorization interface between front-end and back-end layers of
various Huygens ING projects, e.g., Elaborate and implements federated
authentication by communicating with SAML2 enabled Identity Providers (IdP),
such as SURFconext.

We are currently in the process of adding the CLARIN IdP to our pool of
supported Identity Providers.

## How does it work?
Federated-auth is a Java REST-service, based on JAX-RS, implemented using
Jersey. It relies on OpenSAML for SAML messages related to login and assertion
consumption. Externally two URLs are available: ${auth}/login for starting up
the login procedure and ${auth}/acs after authentication to deliver the (signed)
SAML response from the selected IdP.

## Login procedure
* User agent logins in, most likely via Javascript equivalent of:
  `https://secure.huygens.knaw.nl/saml2/login [POST]` with parameter:
  `hsurl={url_to_return_to_after_login}`

* User agent is given SAML `RelayState` and `SAMLRequest` parameters and follows
  chain of redirects to authenticate at selected target IdP ...

* IdP proceeds to authenticate user, provides signed `SAMLResponse` parameter
  and redirects User Agent back to "Authentication Consumer Service" part of
  Huygens federated auth service
  `https://secure.huygens.knaw.nl/acs [POST]`
  parameters: `RelayState` and signed `SAMLResponse`

* Huygens federated auth service verifies SAMLReponse signature against IdP
  public key and sets up local session for Huygens service. User agent is
  provided with session identifier and is redirected (finally) to originally
  requested `hsurl` location.

## Test page (browser)
The [Federated Auth Test](https://secure.huygens.knaw.nl/static/index.html) page
demonstrates a sample service, which shows information about the authenticated
user as obtained through SAML negotiation from the Identity Provider (IdP) which
was used to authenticate.

This testservice can be used by anyone with an account at any of the institutes
connected via SURFconext. After loading, the page will redirect via SURF IdP to
the IdP of any selected institute. Upon providing valid credentials to this IdP,
the user's browser will be redirected to the test page, to present the data
about the authenticated user in the browser.
