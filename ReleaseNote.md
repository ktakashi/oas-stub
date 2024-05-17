Release Notes
=============


Version 2.0.0
---

### Overall architecture change which includes;
- Servlet to reactive
- Netty as underlying server
- Remove purpose specific modules, such as reactive, guice et al.
- Backward incompatible API changes, e.g. plugins

### Connection error failure option
`/options` endpoint with `{"type": "connection"}`

### Static configuration
Allow users to predefine own stub during server start up
instead of calling admin endpoints.

Version 1.3.0
---

- Reactive module
- Spring Boot version 3.2.5 

Version 1.2.0
---

- Guice module support
- Standalone server atop Jetty and Guice
- Testing module using standalone server
- Adding BOM module
- OAS 3.1.x support

Version 1.1.0
---

- Better integration of Spring Boot
- Adding testing module for Spring Boot

Version 1.0.0
---

Initial version of OAS stub