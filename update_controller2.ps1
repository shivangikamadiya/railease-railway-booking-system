$content = Get-Content 'src/main/java/com/railease/controller/BookingController.java' -Raw

# Add the new confirmation endpoint after showPaymentPage method
$newEndpoint = @'

    @GetMapping("/confirmation/{ticketId}")
    @Transactional(readOnly = true)
    public String showConfirmationPage(@PathVariable String ticketId,
                                       HttpSession session,
                                       Model model) {
        User user = (User) session.getAttribute("activeUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Ticket ticket = bookingService.getTicketById(ticketId);

            if (!ticket.getUser().getUserId().equals(user.getUserId())) {
                return "redirect:/user/my-bookings";
            }

            if ("PAID".equals(ticket.getPaymentStatus())) {
                return "redirect:/booking/success/" + ticketId;
            }

            // Eagerly load train to avoid LazyInitializationException
            Train train = ticket.getTrain();
            String trainName = train.getTrainName();
            String trainNumber = String.valueOf(train.getTrainNo());

            model.addAttribute("ticket", ticket);
            model.addAttribute("trainName", trainName);
            model.addAttribute("trainNumber", trainNumber);
            model.addAttribute("activeUser", user);

            return "user/booking-confirmation";

        } catch (Exception e) {
            log.error("Error showing confirmation page: {}", e.getMessage());
            return "redirect:/user/my-bookings";
        }
    }

'@

# Find the position after showPaymentPage method (line 143) and insert the new endpoint
$pattern = '(\s+return "user/payment";\s+\}\s+\}\s+)(@PostMapping\("/process-payment"\))'
$replacement = '$1' + $newEndpoint + '$2'
$content = $content -replace $pattern, $replacement

# Also update the redirect in confirmBooking to go to confirmation page instead of payment
$content = $content -replace 'return "redirect:/booking/payment/" \+ ticket\.getTicketId\(\)', 'return "redirect:/booking/confirmation/" + ticket.getTicketId()'

Set-Content 'src/main/java/com/railease/controller/BookingController.java' $content
Write-Host "Controller updated successfully"