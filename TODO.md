# RailEase Ticket Booking Fix - ticket_price NPE

## Status: [IN PROGRESS]

### 1. [ ] Create TODO.md ✅
### 2. [ ] Edit Train.java - Remove ticketPrice field
### 3. [ ] Edit BookingServiceImpl.java - Replace calculateBaseFare() with strict class lookup
### 4. [ ] Restart Spring Boot: mvn spring-boot:run
### 5. [ ] Test workflow: login → view-trains → details → form → confirm → payment → success
### 6. [ ] Verify fares in DB (run sql/fix-train-fares.sql if needed)
### 7. [ ] Mark complete