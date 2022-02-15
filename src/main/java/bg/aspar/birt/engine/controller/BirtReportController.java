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


	// + http://localhost:8080/reports
	@GetMapping(value = "/reports", produces = "application/json")
	@ResponseBody
	public List<Report> listReports() {
		log.info("REST request to list ALL reports.");

		return reportService.getReports();
	}

	// + http://localhost:8080/reports/reload
	@GetMapping(value = "/reports/reload", produces = "application/json")
	@ResponseBody
	public ResponseEntity<Void> reloadReports(HttpServletResponse response) {
		log.info("REST request to reload reports.");

		try {
			reportService.loadReports();
		} catch (EngineException ex) {
			log.error("There was an error reloading the reports in memory: ", ex);
			return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
		}
		return ResponseEntity.ok().build();
	}

	// TEST URL:
	// + http://localhost:8080/reports/static-generated-example?output=HTML
	// + http://localhost:8080/reports/static-generated-example?output=html
	// + http://localhost:8080/reports/static-generated-example?output=pdf
	//
	// - http://localhost:8080/reports/csv_data_report?output=html -- ERROR
	@GetMapping(value = "/reports/{name}")
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