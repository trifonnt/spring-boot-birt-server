package bg.aspar.birt.engine.controller;

import org.apache.log4j.Logger;
import org.eclipse.birt.report.engine.api.EngineException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import bg.aspar.birt.engine.dto.OutputType;
import bg.aspar.birt.engine.dto.Report;
import bg.aspar.birt.engine.service.BirtReportService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class BirtReportController {

	private static final Logger log = Logger.getLogger(BirtReportController.class);

	@Autowired
	private BirtReportService reportService;

	// TEST URLs:
	// - http://localhost:8080/report
	@GetMapping(value = "/report", produces = "application/json")
	@ResponseBody
	public List<Report> listReports() {
		return reportService.getReports();
	}

	@GetMapping(value = "/report/reload", produces = "application/json")
	@ResponseBody
	public ResponseEntity<Void> reloadReports(HttpServletResponse response) {
		try {
			log.info("Reloading reports");
			reportService.loadReports();
		} catch (EngineException e) {
			log.error("There was an error reloading the reports in memory: ", e);
			return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
		}
		return ResponseEntity.ok().build();
	}

	// TEST URL:
	// - http://localhost:8080/report/static-generated-example?output=HTML
	// - http://localhost:8080/report/static-generated-example?output=html
	// - http://localhost:8080/report/static-generated-example?output=pdf
	//
	// - http://localhost:8080/report/csv_data_report?output=html
	@GetMapping(value = "/report/{name}")
	@ResponseBody
	public void generateFullReport(HttpServletResponse response, HttpServletRequest request
			, @PathVariable("name") String name
			, @RequestParam("output") String output
	) {
		log.info("Generating full report: " + name + "; format: " + output);
		OutputType format = OutputType.from(output);
		reportService.generateMainReport(name, format, response, request);
	}
}