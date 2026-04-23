package com.evbattery.modules.contract.service;

import java.util.Map;

public interface ContractService {
    Map<String, Object> ensureContractForOrder(Long orderId);

    Map<String, Object> listContracts(Long currentUserId, boolean admin, Integer page, Integer size);

    Map<String, Object> verifyContract(String contractNo, byte[] uploadBytes);

    Map<String, Object> verifyContractById(Long contractId);

    byte[] loadContractPdf(Long contractId, Long currentUserId, boolean admin);
}
