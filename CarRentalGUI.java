import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

class Car {
    private String model, carId;
    private double dailyRate;
    private boolean isAvailable = true;

    public Car(String model, String carId, double dailyRate) {
        this.model = model;
        this.carId = carId;
        this.dailyRate = dailyRate;
    }
    public String getModel() { return model; }
    public String getCarId() { return carId; }
    public double getDailyRate() { return dailyRate; }
    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }
    @Override
    public String toString() {
        return model + " (" + carId + ") - $" + dailyRate + "/day" + (isAvailable ? " [Available]" : " [Rented]");
    }
}

class Rental {
    private Car car;
    private String customerName;
    private LocalDateTime pickupDate, returnDate;
    private double totalCost;

    public Rental(Car car, String customerName, LocalDateTime pickupDate, LocalDateTime returnDate) {
        this.car = car;
        this.customerName = customerName;
        this.pickupDate = pickupDate;
        this.returnDate = returnDate;
        this.totalCost = ChronoUnit.DAYS.between(pickupDate, returnDate) * car.getDailyRate();
    }
    public Car getCar() { return car; }
    public String getCustomerName() { return customerName; }
    public LocalDateTime getPickupDate() { return pickupDate; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public double getTotalCost() { return totalCost; }
    public boolean canCancel() {
        long hours = ChronoUnit.HOURS.between(LocalDateTime.now(), pickupDate);
        return hours >= 0 && hours <= 24;
    }
    public String getSummary() {
        return customerName + " | " + car.getModel() + " | Pickup: " + pickupDate + " | Return: " + returnDate + " | $" + totalCost;
    }
}

public class CarRentalGUI extends JFrame {
    private DefaultListModel<Car> carListModel = new DefaultListModel<>();
    private DefaultListModel<String> rentalListModel = new DefaultListModel<>();
    private List<Rental> rentals = new ArrayList<>();
    private JList<Car> carJList;
    private JTextArea outputArea;

    public CarRentalGUI() {
        setTitle("🚗 Cool Car Rental System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(Color.decode("#1e1e2f"));

        // Heading
        JLabel title = new JLabel("🚗 Car Rental System", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(Color.decode("#00e676"));
        title.setBorder(new EmptyBorder(10, 0, 10, 0));
        add(title, BorderLayout.NORTH);

        // Left Panel - Car List
        carJList = new JList<>(carListModel);
        carJList.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        carJList.setBackground(Color.decode("#282a36"));
        carJList.setForeground(Color.decode("#f8f8f2"));
        JScrollPane carScroll = new JScrollPane(carJList);
        carScroll.setBorder(BorderFactory.createTitledBorder("Available Cars"));
        add(carScroll, BorderLayout.WEST);

        // Right Panel - Rental Info
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        outputArea.setBackground(Color.decode("#2e2e38"));
        outputArea.setForeground(Color.decode("#f1f1f1"));
        outputArea.setEditable(false);
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Rental Status"));
        add(outputScroll, BorderLayout.CENTER);

        // Bottom Panel - Buttons
        JPanel controlPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        controlPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        controlPanel.setBackground(Color.decode("#1e1e2f"));

        JButton rentBtn = new JButton("Rent");
        JButton returnBtn = new JButton("Return");
        JButton cancelBtn = new JButton("Cancel");
        JButton refreshBtn = new JButton("Refresh");

        controlPanel.add(rentBtn);
        controlPanel.add(returnBtn);
        controlPanel.add(cancelBtn);
        controlPanel.add(refreshBtn);

        add(controlPanel, BorderLayout.SOUTH);

        // Button Actions
        rentBtn.addActionListener(e -> rentCar());
        returnBtn.addActionListener(e -> returnCar());
        cancelBtn.addActionListener(e -> cancelRental());
        refreshBtn.addActionListener(e -> refreshList());

        // Demo cars
        carListModel.addElement(new Car("Toyota Camry", "CAM123", 50));
        carListModel.addElement(new Car("Honda Civic", "CIV456", 45));
        carListModel.addElement(new Car("Ford F-150", "F150", 70));
    }

    private void rentCar() {
        Car selected = carJList.getSelectedValue();
        if (selected == null || !selected.isAvailable()) {
            showMsg("Select an available car.");
            return;
        }
        String name = JOptionPane.showInputDialog(this, "Enter Customer Name:");
        if (name == null || name.isEmpty()) return;

        String pickupStr = JOptionPane.showInputDialog(this, "Enter Pickup Date (YYYY-MM-DDTHH:MM):");
        String returnStr = JOptionPane.showInputDialog(this, "Enter Return Date (YYYY-MM-DDTHH:MM):");
        try {
            LocalDateTime pickup = LocalDateTime.parse(pickupStr);
            LocalDateTime ret = LocalDateTime.parse(returnStr);
            if (ret.isBefore(pickup)) throw new Exception();
            Rental rental = new Rental(selected, name, pickup, ret);
            selected.setAvailable(false);
            rentals.add(rental);
            showMsg("Rented Successfully!\n" + rental.getSummary());
        } catch (Exception ex) {
            showMsg("Invalid date format or return before pickup.");
        }
    }

    private void returnCar() {
        Car selected = carJList.getSelectedValue();
        if (selected == null || selected.isAvailable()) {
            showMsg("This car is not rented.");
            return;
        }
        selected.setAvailable(true);
        rentals.removeIf(r -> r.getCar().equals(selected));
        showMsg("Car returned successfully.");
    }

    private void cancelRental() {
        Car selected = carJList.getSelectedValue();
        if (selected == null || selected.isAvailable()) {
            showMsg("This car is not currently rented.");
            return;
        }
        for (Rental r : rentals) {
            if (r.getCar().equals(selected)) {
                if (r.canCancel()) {
                    rentals.remove(r);
                    selected.setAvailable(true);
                    showMsg("Rental cancelled successfully.");
                    return;
                } else {
                    showMsg("Cannot cancel. Pickup in more than 24h or already started.");
                    return;
                }
            }
        }
        showMsg("Rental not found.");
    }

    private void refreshList() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nActive Rentals:\n");
        for (Rental r : rentals) {
            sb.append(r.getSummary()).append("\n");
        }
        outputArea.setText(sb.toString());
        carJList.repaint();
    }

    private void showMsg(String msg) {
        JOptionPane.showMessageDialog(this, msg);
        refreshList();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CarRentalGUI().setVisible(true));
    }
}
