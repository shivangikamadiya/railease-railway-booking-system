$content = Get-Content 'src/main/java/com/railease/service/impl/BookingServiceImpl.java' -Raw
$oldCode = @'
    private double calculateBaseFare(Train train, String classType, int numberOfSeats) {
        // Using ticketPrice as base fare since we don't have separate class fares
        return train.getTicketPrice() * numberOfSeats;
    }
'@
$newCode = @'
    private double calculateBaseFare(Train train, String classType, int numberOfSeats) {
        // Use class-specific fare based on classType
        double classFare;
        if ("AC".equalsIgnoreCase(classType)) {
            classFare = train.getAcFare() != null ? train.getAcFare() : train.getTicketPrice();
        } else if ("SLEEPER".equalsIgnoreCase(classType)) {
            classFare = train.getSleepperFare() != null ? train.getSleepperFare() : train.getTicketPrice();
        } else if ("GENERAL".equalsIgnoreCase(classType)) {
            classFare = train.getGeneralFare() != null ? train.getGeneralFare() : train.getTicketPrice();
        } else {
            // Default to ticketPrice if classType is not recognized
            classFare = train.getTicketPrice();
        }
        return classFare * numberOfSeats;
    }
'@
$content = $content -replace [regex]::Escape($oldCode), $newCode
Set-Content 'src/main/java/com/railease/service/impl/BookingServiceImpl.java' $content
Write-Host "File updated successfully"