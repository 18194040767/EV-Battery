package com.evbattery.modules.contract.controller;

import com.evbattery.common.AuthUserContext;
import com.evbattery.common.result.Result;
import com.evbattery.modules.admin.service.AdminService;
import com.evbattery.modules.contract.service.ContractService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.Map;

@RestController
@RequestMapping("/api/contract")
public class ContractController {

    @Resource
    private ContractService contractService;

    @Resource
    private AdminService adminService;

    @PostMapping("/generate")
    public Result<Object> generate(@RequestParam Long orderId) {
        return Result.success(contractService.ensureContractForOrder(orderId));
    }

    @PostMapping("/generate/download")
    public ResponseEntity<byte[]> generateAndDownload(@RequestParam Long orderId) {
        Long currentUserId = AuthUserContext.getCurrentUserId();
        Map<String, Object> contract = contractService.ensureContractForOrder(orderId);
        Long contractId = Long.parseLong(String.valueOf(contract.get("id")));
        byte[] bytes = contractService.loadContractPdf(contractId, currentUserId, adminService.isAdmin(currentUserId));
        String contractNo = String.valueOf(contract.getOrDefault("contractNo", "contract-" + contractId));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition("attachment", "contract-" + contractId + ".pdf", contractNo + ".pdf"))
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .body(bytes);
    }

    @GetMapping("/list")
    public Result<Object> list(@RequestParam(required = false, defaultValue = "1") Integer page,
                               @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long currentUserId = AuthUserContext.getCurrentUserId();
        return Result.success(contractService.listContracts(currentUserId, adminService.isAdmin(currentUserId), page, size));
    }

    @GetMapping("/verify/{id}")
    public Result<Object> verifyById(@PathVariable Long id) {
        return Result.success(contractService.verifyContractById(id));
    }

    @PostMapping("/verify")
    public Result<Object> verify(@RequestParam String contractNo,
                                 @RequestPart(required = false) MultipartFile file) throws Exception {
        byte[] uploadBytes = file == null ? null : file.getBytes();
        return Result.success(contractService.verifyContract(contractNo, uploadBytes));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Long currentUserId = AuthUserContext.getCurrentUserId();
        byte[] bytes = contractService.loadContractPdf(id, currentUserId, adminService.isAdmin(currentUserId));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition("attachment", "contract-" + id + ".pdf", "contract-" + id + ".pdf"))
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .body(bytes);
    }

    @GetMapping("/preview/{id}")
    public ResponseEntity<byte[]> preview(@PathVariable Long id) {
        Long currentUserId = AuthUserContext.getCurrentUserId();
        byte[] bytes = contractService.loadContractPdf(id, currentUserId, adminService.isAdmin(currentUserId));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition("inline", "contract-" + id + ".pdf", "contract-" + id + ".pdf"))
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .body(bytes);
    }

    private String contentDisposition(String disposition, String fallbackFilename, String filename) {
        return disposition + "; filename=\"" + sanitizeAsciiFilename(fallbackFilename) + "\"; filename*=UTF-8''" + encodeFilename(filename);
    }

    private String sanitizeAsciiFilename(String filename) {
        return String.valueOf(filename == null ? "document.pdf" : filename).replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String encodeFilename(String filename) {
        try {
            return URLEncoder.encode(String.valueOf(filename == null ? "document.pdf" : filename), StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (Exception ex) {
            return "document.pdf";
        }
    }
}
