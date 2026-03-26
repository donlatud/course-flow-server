package com.techup.course_flow_server.controller;

import com.techup.course_flow_server.entity.TestItem;
import com.techup.course_flow_server.repository.TestItemRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-items")
public class TestItemController {

    private final TestItemRepository testItemRepository;

    public TestItemController(TestItemRepository testItemRepository) {
        this.testItemRepository = testItemRepository;
    }

    @GetMapping
    public List<TestItem> getAll() {
        return testItemRepository.findAll();
    }
}
