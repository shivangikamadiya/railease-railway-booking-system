**Primary fixes complete** 🎉

**fixed/BookingServiceImplFixed.java**:
- ✅ NPE: All `train.getTicketPrice()` → safe defaults
- ✅ Race condition: Class capacity - COUNT(*) queries
- ✅ ClassType validation

**Test**: Run booking flow. Should complete without transaction rollback.

**Remaining**: Controller parameter cleanup (optional).

**Demo**: `mvn spring-boot:run` → test http://localhost:8080/booking/process?trainNo=123