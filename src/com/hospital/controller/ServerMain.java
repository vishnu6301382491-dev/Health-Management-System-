package com.hospital.controller;

import com.hospital.batch.DataGeneratorService;
import com.hospital.dao.*;
import com.hospital.model.*;
import com.hospital.service.*;
import com.hospital.util.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.*;

public class ServerMain {
    private static final int PORT = 8080;

    private static HospitalService hospitalService = new HospitalService();
    private static DoctorService doctorService = new DoctorService();
    private static PatientService patientService = new PatientService();
    private static AppointmentService appointmentService = new AppointmentService();
    private static ReportService reportService = new ReportService();
    private static FeeManagementService feeService = new FeeManagementService();
    private static PatientDAO patientDAO = new PatientDAO();

    private static EmergencyDAO emergencyDAO = new EmergencyDAO();
    private static PharmacyDAO pharmacyDAO = new PharmacyDAO();
    private static InsuranceDAO insuranceDAO = new InsuranceDAO();
    private static StaffDAO staffDAO = new StaffDAO();
    private static AuditDAO auditDAO = new AuditDAO();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/auth/login", new AuthHandler());
        server.createContext("/api/hospitals", new HospitalsHandler());
        server.createContext("/api/hospitals/branches", new BranchesHandler());
        server.createContext("/api/doctors", new DoctorsHandler());
        server.createContext("/api/patients", new PatientsHandler());
        server.createContext("/api/patients/lookup", new PatientLookupHandler());
        server.createContext("/api/appointments", new AppointmentsHandler());
        server.createContext("/api/appointments/slots", new SlotsHandler());
        server.createContext("/api/appointments/action", new AppointmentActionHandler());
        server.createContext("/api/appointments/calculate-fee", new CalculateFeeHandler());
        server.createContext("/api/dashboard/stats", new DashboardStatsHandler());
        server.createContext("/api/emergency", new EmergencyHandler());
        server.createContext("/api/pharmacy", new PharmacyHandler());
        server.createContext("/api/bloodbank", new BloodBankHandler());
        server.createContext("/api/insurance", new InsuranceHandler());
        server.createContext("/api/staff", new StaffHandler());
        server.createContext("/api/audit", new AuditHandler());
        server.createContext("/api/departments", new DepartmentsHandler());
        server.createContext("/api/lab", new LabHandler());
        server.createContext("/api/billing", new BillingHandler());
        server.createContext("/api/import/hospitals", new ImportHospitalsHandler());
        server.createContext("/api/admin/generate-data", new DataGeneratorHandler());
        server.createContext("/api/admin/generate-data/status", new DataGeneratorStatusHandler());
        server.createContext("/api/admin/fees/bulk-update", new BulkFeeUpdateHandler());
        server.createContext("/api/admin/performance", new PerformanceDiagnosticsHandler());
        server.createContext("/api/doctors/search", new DoctorSearchHandler());
        server.createContext("/api/doctors/fees", new DoctorFeesHandler());
        server.createContext("/api/hospitals/search", new HospitalSearchHandler());
        server.createContext("/api/appointments/availability", new AvailabilityHandler());
        server.createContext("/api/appointments/slots/hold", new SlotHoldHandler());
        server.createContext("/api/admin/diagnostics/slots", new SlotDiagnosticsHandler());

        server.createContext("/", new StaticFileHandler());

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(16));
        server.start();

        auditDAO.log(1, "superadmin", null, "PLATFORM_START", "SYSTEM", "AURA Health Multi-Hospital Platform initialized on port " + PORT);
        System.out.println("=================================================");
        System.out.println("AURA HEALTH MULTI-HOSPITAL SYSTEM RUNNING");
        System.out.println("URL: http://localhost:" + PORT);
        System.out.println("=================================================");
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                boolean dbConnected = false;
                try (Connection conn = DBConnection.getConnection()) {
                    dbConnected = (conn != null && !conn.isClosed());
                } catch (Exception ignored) {}

                Map<String, Object> resp = new HashMap<>();
                resp.put("status", dbConnected ? "UP" : "DEGRADED");
                resp.put("service", "AURA HEALTH Enterprise Platform");
                resp.put("database", dbConnected ? "CONNECTED" : "DISCONNECTED");
                resp.put("port", PORT);
                resp.put("timestamp", new Date());

                sendResponse(exchange, 200, toJson(resp));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/favicon.ico")) {
                exchange.getResponseHeaders().set("Content-Type", "image/x-icon");
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().close();
                return;
            }
            if (path.equals("/")) path = "/index.html";

            File file = new File("frontend" + path);
            if (!file.exists() || file.isDirectory()) {
                sendResponse(exchange, 404, "{\"error\": \"File not found\"}");
                return;
            }

            String contentType = getContentType(path);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, file.length());

            try (OutputStream os = exchange.getResponseBody(); FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        }

        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".json")) return "application/json";
            return "text/plain";
        }
    }

    static class AuthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseJsonSimple(body);
                String username = data.get("username");
                String password = data.get("password");

                User user = AuthService.authenticate(username, password);
                if (user != null) {
                    auditDAO.log(user.getId(), user.getUsername(), user.getHospitalId(), "USER_LOGIN", "AUTH", "User " + username + " logged in successfully.");
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("success", true);
                    resp.put("userId", user.getId());
                    resp.put("username", user.getUsername());
                    resp.put("role", user.getRole());
                    resp.put("email", user.getEmail());
                    resp.put("hospitalId", user.getHospitalId());
                    sendResponse(exchange, 200, toJson(resp));
                } else {
                    sendResponse(exchange, 401, "{\"success\": false, \"message\": \"Invalid credentials\"}");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class HospitalsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                String search = q.get("search");
                String city = q.get("city");
                String type = q.get("type");
                Double minRating = q.containsKey("minRating") ? Double.parseDouble(q.get("minRating")) : null;
                Boolean pharmacy = q.containsKey("pharmacy") ? Boolean.parseBoolean(q.get("pharmacy")) : null;
                Boolean bloodBank = q.containsKey("bloodBank") ? Boolean.parseBoolean(q.get("bloodBank")) : null;
                Boolean ambulance = q.containsKey("ambulance") ? Boolean.parseBoolean(q.get("ambulance")) : null;
                Boolean insurance = q.containsKey("insurance") ? Boolean.parseBoolean(q.get("insurance")) : null;

                List<Hospital> list = hospitalService.searchHospitals(search, city, type, minRating, pharmacy, bloodBank, ambulance, insurance);
                sendResponse(exchange, 200, toJson(list));
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Hospital h = parseJsonObject(body, Hospital.class);
                boolean success = hospitalService.registerHospital(h);
                if (success) {
                    auditDAO.log(1, "superadmin", null, "REGISTER_HOSPITAL", "HOSPITALS", "Registered hospital " + h.getName());
                    sendResponse(exchange, 201, "{\"success\": true}");
                } else {
                    sendResponse(exchange, 400, "{\"success\": false}");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class BranchesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                int hospId = Integer.parseInt(q.getOrDefault("hospitalId", "1"));
                List<HospitalBranch> list = hospitalService.getBranches(hospId);
                sendResponse(exchange, 200, toJson(list));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class DoctorsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                String search = q.get("search");
                String city = q.get("city");
                String state = q.get("state");
                String spec = q.get("specialization");
                Integer hospitalId = q.containsKey("hospitalId") ? Integer.parseInt(q.get("hospitalId")) : null;
                Double minFee = q.containsKey("minFee") ? Double.parseDouble(q.get("minFee")) : null;
                Double maxFee = q.containsKey("maxFee") ? Double.parseDouble(q.get("maxFee")) : null;
                Double minRating = q.containsKey("minRating") ? Double.parseDouble(q.get("minRating")) : null;
                Integer minExp = q.containsKey("minExp") ? Integer.parseInt(q.get("minExp")) : null;
                String gender = q.get("gender");
                String sortBy = q.get("sortBy");

                if (q.containsKey("page") || q.containsKey("pageSize") || q.containsKey("city") || q.containsKey("minFee")) {
                    int page = q.containsKey("page") ? Integer.parseInt(q.get("page")) : 1;
                    int pageSize = q.containsKey("pageSize") ? Integer.parseInt(q.get("pageSize")) : 20;

                    PaginatedResult<Doctor> result = doctorService.getDoctorsPaginated(search, city, state, spec, hospitalId, minFee, maxFee, minRating, minExp, gender, page, pageSize, sortBy, "DESC");
                    sendResponse(exchange, 200, toJson(result));
                } else {
                    List<Doctor> list = doctorService.getAllDoctors(search, null, spec, hospitalId, "ACTIVE");
                    sendResponse(exchange, 200, toJson(list));
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class PatientsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                List<Patient> list = patientService.getAllPatients(q.get("search"));
                sendResponse(exchange, 200, toJson(list));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class PatientLookupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                String query = q.get("query");
                Patient p = patientDAO.getByCodeOrPhone(query);
                if (p != null) {
                    sendResponse(exchange, 200, toJson(p));
                } else {
                    sendResponse(exchange, 404, "{\"found\": false}");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class AppointmentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                    Integer patientId = parseInteger(q.get("patientId"));
                    Integer doctorId = parseInteger(q.get("doctorId"));
                    Integer hospitalId = parseInteger(q.get("hospitalId"));
                    String status = q.get("status");
                    String date = q.get("date");

                    List<Appointment> list = appointmentService.getAllAppointments(patientId, doctorId, hospitalId, status, date);
                    sendResponse(exchange, 200, toJson(list));
                } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String body = readRequestBody(exchange);
                    Appointment app = parseJsonObject(body, Appointment.class);

                    if (app == null) {
                        sendResponse(exchange, 400, "{\"success\": false, \"errorCode\": \"INVALID_JSON\", \"message\": \"Malformed appointment payload.\"}");
                        return;
                    }

                    if (app.getDoctorId() <= 0) {
                        sendResponse(exchange, 400, "{\"success\": false, \"errorCode\": \"INVALID_DOCTOR\", \"message\": \"Please select a valid doctor for the appointment.\", \"field\": \"doctorId\"}");
                        return;
                    }

                    if (app.getAppointmentDate() == null || app.getAppointmentDate().trim().isEmpty()) {
                        sendResponse(exchange, 400, "{\"success\": false, \"errorCode\": \"INVALID_DATE\", \"message\": \"Please select an appointment date.\", \"field\": \"appointmentDate\"}");
                        return;
                    }

                    if (app.getTimeSlot() == null || app.getTimeSlot().trim().isEmpty()) {
                        sendResponse(exchange, 400, "{\"success\": false, \"errorCode\": \"INVALID_SLOT\", \"message\": \"Please select an available time slot.\", \"field\": \"timeSlot\"}");
                        return;
                    }

                    com.hospital.dao.AppointmentDAO.BookingResult result = appointmentService.bookAppointmentTransaction(app);
                    if (result.isSuccess()) {
                        auditDAO.log(app.getPatientId(), "patient", app.getHospitalId(), "BOOK_APPOINTMENT", "APPOINTMENTS", "Booked appointment " + result.getAppointmentCode());
                        sendResponse(exchange, 201, "{\"success\": true, \"message\": \"Appointment booked successfully\", \"appointmentCode\": \"" + result.getAppointmentCode() + "\"}");
                    } else {
                        sendResponse(exchange, 400, "{\"success\": false, \"errorCode\": \"" + result.getErrorCode() + "\", \"message\": \"" + (result.getMessage() != null ? result.getMessage().replace("\"", "'") : "Booking failed") + "\"}");
                    }
                } else {
                    sendResponse(exchange, 405, "Method Not Allowed");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 400, "{\"success\": false, \"errorCode\": \"APPOINTMENT_QUERY_ERROR\", \"message\": \"" + (e.getMessage() != null ? e.getMessage().replace("\"", "'") : "Appointment processing error") + "\"}");
            }
        }
    }

    static class CalculateFeeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                int docId = Integer.parseInt(q.getOrDefault("doctorId", "1"));
                int hospId = Integer.parseInt(q.getOrDefault("hospitalId", "1"));
                String type = q.getOrDefault("appointmentType", "In-Person");

                Map<String, Object> res = feeService.calculateAppointmentFee(docId, hospId, type);
                sendResponse(exchange, 200, toJson(res));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class AppointmentActionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseJsonSimple(body);
                int aptId = Integer.parseInt(data.get("appointmentId"));
                String action = data.get("action");

                boolean success = appointmentService.updateAppointmentStatus(aptId, action);
                if (success) {
                    auditDAO.log(1, "system", null, "UPDATE_APPOINTMENT", "APPOINTMENTS", "Updated appointment #" + aptId + " to " + action);
                    sendResponse(exchange, 200, "{\"success\": true}");
                } else {
                    sendResponse(exchange, 400, "{\"success\": false}");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class SlotsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                int doctorId = Integer.parseInt(q.getOrDefault("doctorId", "1"));
                String date = q.getOrDefault("date", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));

                List<Map<String, Object>> slots = appointmentService.getAvailableSlots(doctorId, date);
                sendResponse(exchange, 200, toJson(slots));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class DashboardStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                Integer hospitalId = q.containsKey("hospitalId") ? Integer.parseInt(q.get("hospitalId")) : null;
                DashboardStats stats = reportService.getDashboardStats(hospitalId);
                sendResponse(exchange, 200, toJson(stats));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class EmergencyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                int hospitalId = Integer.parseInt(q.getOrDefault("hospitalId", "1"));
                List<Ambulance> list = emergencyDAO.getAmbulances(hospitalId);
                sendResponse(exchange, 200, toJson(list));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class PharmacyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<Medicine> list = pharmacyDAO.getMedicines("");
                sendResponse(exchange, 200, toJson(list));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class BloodBankHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                int hospitalId = Integer.parseInt(q.getOrDefault("hospitalId", "1"));
                List<BloodBank> list = emergencyDAO.getBloodBankUnits(hospitalId);
                sendResponse(exchange, 200, toJson(list));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class InsuranceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<Map<String, Object>> list = insuranceDAO.getAllClaims();
                sendResponse(exchange, 200, toJson(list));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class StaffHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<Map<String, Object>> list = staffDAO.getAllStaff();
                sendResponse(exchange, 200, toJson(list));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class AuditHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<Map<String, Object>> list = auditDAO.getRecentLogs(50);
                sendResponse(exchange, 200, toJson(list));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class DepartmentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<Map<String, Object>> list = Arrays.asList(
                    createDeptMap(1, "DEP-CARD", "Cardiology", "Heart & vascular surgical care."),
                    createDeptMap(2, "DEP-NEUR", "Neurology", "Brain, stroke & neurosciences."),
                    createDeptMap(3, "DEP-ORTH", "Orthopedics", "Robotic joint replacement & trauma."),
                    createDeptMap(4, "DEP-ONCO", "Oncology", "Targeted cancer care & chemotherapy.")
                );
                sendResponse(exchange, 200, toJson(list));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
        private Map<String, Object> createDeptMap(int id, String code, String name, String desc) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", id); m.put("deptCode", code); m.put("name", name); m.put("description", desc);
            return m;
        }
    }

    static class LabHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<Map<String, Object>> list = Arrays.asList(
                    createLabMap("LAB00001", "Complete Blood Count (CBC)", "Haematology", "Blood", 450.00, "Completed"),
                    createLabMap("LAB00002", "Brain MRI Scan", "Radiology", "Imaging", 3500.00, "Processing")
                );
                sendResponse(exchange, 200, toJson(list));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
        private Map<String, Object> createLabMap(String code, String name, String cat, String sample, double price, String status) {
            Map<String, Object> m = new HashMap<>();
            m.put("testCode", code); m.put("testName", name); m.put("category", cat); m.put("sampleType", sample); m.put("price", price); m.put("status", status);
            return m;
        }
    }

    static class BillingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<Map<String, Object>> list = Arrays.asList(
                    createBillMap("INV00001", 2682.50, 2682.50, "Paid", "2026-08-18")
                );
                sendResponse(exchange, 200, toJson(list));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
        private Map<String, Object> createBillMap(String code, double total, double paid, String status, String date) {
            Map<String, Object> m = new HashMap<>();
            m.put("invoiceCode", code); m.put("totalAmount", total); m.put("paidAmount", paid); m.put("paymentStatus", status); m.put("invoiceDate", date);
            return m;
        }
    }

    static class ImportHospitalsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                List<Hospital> imported = CsvUtil.parseHospitalsCsv(body);
                int count = 0;
                for (Hospital h : imported) {
                    if (hospitalService.registerHospital(h)) count++;
                }
                auditDAO.log(1, "superadmin", null, "CSV_BATCH_IMPORT", "DATA_IMPORT", "Imported " + count + " hospitals via CSV.");
                sendResponse(exchange, 200, "{\"success\": true, \"imported\": " + count + "}");
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class DataGeneratorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseJsonSimple(body);
                int hospCount = data.containsKey("hospitals") ? Integer.parseInt(data.get("hospitals")) : 1000;
                int docCount = data.containsKey("doctors") ? Integer.parseInt(data.get("doctors")) : 100000;

                DataGeneratorService.startGenerationAsync(hospCount, docCount);
                auditDAO.log(1, "superadmin", null, "DATASET_GENERATION_START", "ADMIN", "Triggered generation of " + hospCount + " hospitals and " + docCount + " doctors.");
                sendResponse(exchange, 200, "{\"success\": true, \"message\": \"Batch data generation started in background.\"}");
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class DataGeneratorStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, Object> status = DataGeneratorService.getProgressStatus();
                sendResponse(exchange, 200, toJson(status));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class BulkFeeUpdateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseJsonSimple(body);
                String spec = data.get("specialization");
                String city = data.get("city");
                Double fee = data.containsKey("fee") ? Double.parseDouble(data.get("fee")) : null;
                Double mult = data.containsKey("multiplier") ? Double.parseDouble(data.get("multiplier")) : null;

                int updated = feeService.bulkUpdateFees(spec, city, fee, mult);
                auditDAO.log(1, "superadmin", null, "BULK_FEE_UPDATE", "FEE_MANAGEMENT", "Updated consultation fees for " + updated + " doctors.");
                sendResponse(exchange, 200, "{\"success\": true, \"updated\": " + updated + "}");
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) builder.append(line);
            return builder.toString();
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    static class DoctorSearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                String query = q.get("q");
                Integer hospitalId = parseInteger(q.get("hospitalId"));
                String spec = q.get("specialization");
                int limit = parseInteger(q.get("limit")) != null ? parseInteger(q.get("limit")) : 20;

                DoctorDAO dao = new DoctorDAO();
                List<Map<String, Object>> list = dao.searchDoctorsFast(query, hospitalId, spec, limit);
                sendResponse(exchange, 200, toJson(list));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class DoctorFeesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                int docId = parseInteger(q.get("doctorId")) != null ? parseInteger(q.get("doctorId")) : 1;

                DoctorDAO dao = new DoctorDAO();
                Map<String, Object> res = dao.getDoctorFees(docId);
                sendResponse(exchange, 200, toJson(res));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class HospitalSearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                String query = q.get("q");
                int limit = parseInteger(q.get("limit")) != null ? parseInteger(q.get("limit")) : 20;

                HospitalDAO dao = new HospitalDAO();
                List<Map<String, Object>> list = dao.searchHospitalsFast(query, limit);
                sendResponse(exchange, 200, toJson(list));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class PerformanceDiagnosticsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Runtime runtime = Runtime.getRuntime();
                long totalMem = runtime.totalMemory() / (1024 * 1024);
                long freeMem = runtime.freeMemory() / (1024 * 1024);
                long usedMem = totalMem - freeMem;
                long maxMem = runtime.maxMemory() / (1024 * 1024);

                boolean dbConnected = false;
                try (Connection conn = DBConnection.getConnection()) {
                    dbConnected = (conn != null && !conn.isClosed());
                } catch (Exception ignored) {}

                Map<String, Object> map = new HashMap<>();
                map.put("status", "OPTIMIZED");
                map.put("backendEngine", "AURA Enterprise Java 21");
                map.put("databaseStatus", dbConnected ? "HEALTHY" : "DISCONNECTED");
                map.put("usedMemoryMb", usedMem);
                map.put("totalMemoryMb", totalMem);
                map.put("maxMemoryMb", maxMem);
                map.put("activeThreads", Thread.activeCount());
                map.put("serverPort", PORT);
                map.put("performanceMode", "HIGH_SCALE_1M_OPTIMIZED");
                map.put("timestamp", new Date());

                sendResponse(exchange, 200, toJson(map));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class AvailabilityHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                int docId = parseInteger(q.get("doctorId")) != null ? parseInteger(q.get("doctorId")) : 1;
                int hospId = parseInteger(q.get("hospitalId")) != null ? parseInteger(q.get("hospitalId")) : 1;
                int branchId = parseInteger(q.get("branchId")) != null ? parseInteger(q.get("branchId")) : 1;
                String date = q.get("date");

                Map<String, Object> avail = appointmentService.getAvailability(docId, hospId, branchId, date);
                sendResponse(exchange, 200, toJson(avail));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class SlotHoldHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseJsonSimple(body);
                String slotId = data.get("slotId");
                String patientId = data.get("patientId") != null ? data.get("patientId") : "1";
                int docId = parseInteger(data.get("doctorId")) != null ? parseInteger(data.get("doctorId")) : 1;
                int hospId = parseInteger(data.get("hospitalId")) != null ? parseInteger(data.get("hospitalId")) : 1;
                String date = data.get("date");
                String displayTime = data.get("displayTime");

                boolean held = appointmentService.holdSlot(slotId, patientId, docId, hospId, date, displayTime);
                if (held) {
                    sendResponse(exchange, 200, "{\"success\": true, \"message\": \"Slot held for 5 minutes.\"}");
                } else {
                    sendResponse(exchange, 400, "{\"success\": false, \"message\": \"Slot is unavailable for hold.\"}");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    static class SlotDiagnosticsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                int docId = parseInteger(q.get("doctorId")) != null ? parseInteger(q.get("doctorId")) : 1;
                String date = q.get("date");

                List<Map<String, Object>> list = appointmentService.getSlotDiagnostics(docId, date);
                sendResponse(exchange, 200, toJson(list));
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                try {
                    map.put(pair[0], URLDecoder.decode(pair[1], "UTF-8"));
                } catch (Exception e) { map.put(pair[0], pair[1]); }
            }
        }
        return map;
    }

    private static Map<String, String> parseJsonSimple(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"[^\"]*\"|[^,\\}\\]]+)");
        java.util.regex.Matcher matcher = pattern.matcher(json);
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String val = matcher.group(2).trim();
            if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                val = val.substring(1, val.length() - 1);
            }
            map.put(key, val);
        }
        return map;
    }

    private static Integer parseInteger(String val) {
        if (val == null || val.trim().isEmpty() || "undefined".equalsIgnoreCase(val) || "null".equalsIgnoreCase(val) || "NaN".equalsIgnoreCase(val)) {
            return null;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static <T> T parseJsonObject(String json, Class<T> clazz) {
        try {
            Map<String, String> map = parseJsonSimple(json);
            T instance = clazz.getDeclaredConstructor().newInstance();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String fieldName = entry.getKey();
                String val = entry.getValue();
                try {
                    java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    if (field.getType() == int.class || field.getType() == Integer.class) {
                        field.set(instance, Integer.parseInt(val));
                    } else if (field.getType() == double.class || field.getType() == Double.class) {
                        field.set(instance, Double.parseDouble(val));
                    } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                        field.set(instance, Boolean.parseBoolean(val));
                    } else if (field.getType() == String.class) {
                        field.set(instance, val);
                    }
                } catch (Exception ignored) {}
            }
            return instance;
        } catch (Exception e) {
            return null;
        }
    }

    private static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof Date) {
            return "\"" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) obj) + "\"";
        }
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                sb.append(toJson(list.get(i)));
                if (i < list.size() - 1) sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            int i = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sb.append("\"").append(entry.getKey()).append("\":").append(toJson(entry.getValue()));
                if (i < map.size() - 1) sb.append(",");
                i++;
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof String) {
            return "\"" + ((String) obj).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        
        // POJO serialization
        StringBuilder sb = new StringBuilder("{");
        java.lang.reflect.Field[] fields = obj.getClass().getDeclaredFields();
        int count = 0;
        for (java.lang.reflect.Field f : fields) {
            f.setAccessible(true);
            try {
                Object val = f.get(obj);
                if (val != null) {
                    if (count > 0) sb.append(",");
                    sb.append("\"").append(f.getName()).append("\":").append(toJson(val));
                    count++;
                }
            } catch (Exception ignored) {}
        }
        sb.append("}");
        return sb.toString();
    }
}
