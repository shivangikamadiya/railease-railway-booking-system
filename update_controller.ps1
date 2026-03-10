$content = Get-Content 'src/main/java/com/railease/controller/BookingController.java' -Raw

# Add the new confirmation endpoint after the showPaymentPage method
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

# Find the position after showPaymentPage method and before processPayment method
$pattern = '(@GetMapping\("/payment/\{ticketId\}"\).*?return "user/payment";\s*\}.*?)\s*(@PostMapping\("/process-payment"\))'
if ($content -match $pattern) {
    $content = $content -replace $pattern, "$1$newEndpoint`n$2"
    Set-Content 'src/main/java/com/railease/controller/BookingController.java' $content
    Write-Host "Controller updated successfully"
} else {
    Write-Host "Pattern not found, trying alternative approach"
}