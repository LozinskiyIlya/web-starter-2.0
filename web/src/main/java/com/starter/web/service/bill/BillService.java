package com.starter.web.service.bill;


import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Group;
import com.starter.web.fragments.BillAssistantResponse;
import com.starter.web.service.openai.OpenAiAssistant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {

    private final OpenAiAssistant openAiAssistant;

    public Bill addBill(Group group, BillAssistantResponse assistantResponse) {
        return null;
    }
}
