package bg.aspar.birt.engine.service;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.report.engine.api.*;
import org.springframework.beans.factory.DisposableBean;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
//import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import bg.aspar.birt.engine.dto.OutputType;
import bg.aspar.birt.engine.dto.Report;

import javax.annotation.PostConstruct;
//import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.OutputStream;
import java.util.*;

@Service
public class BirtReportService implements ApplicationContextAware, DisposableBean {

	@Value("${reports.relative.path}")
	private String reportsPath;

	@Value("${images.relative.path}")
	private String imagesPath;

	private HTMLServerImageHandler htmlImageHandler = new HTMLServerImageHandler();

//	@Autowired
//	private ResourceLoader resourceLoader;
//	@Autowired
//	private ServletContext servletContext;

	private IReportEngine birtEngine;
	private ApplicationContext context;
	private String imageFolder;

	private Map<String, IReportRunnable> reports = new HashMap<>();

	@SuppressWarnings("unchecked")
	@PostConstruct
	protected void initialize() throws BirtException {
		EngineConfig config = new EngineConfig();
		config.getAppContext().put("spring", this.context);
		Platform.startup(config);
		IReportEngineFactory factory = (IReportEngineFactory) Platform
				.createFactoryObject(IReportEngineFactory.EXTENSION_REPORT_ENGINE_FACTORY);
		birtEngine = factory.createReportEngine(config);
		imageFolder = System.getProperty("user.dir") + File.separatorChar + reportsPath + imagesPath;
		loadReports();
	}

	@Override
	public void setApplicationContext(ApplicationContext context) {
		this.context = context;
	}

	/**
	 * Load report files to memory
	 *
	 */
	public void loadReports() throws EngineException {
		File folder = new File(reportsPath);
		for (String file : Objects.requireNonNull(folder.list())) {
			if (!file.endsWith(".rptdesign")) {
				continue;
			}

			reports.put(file.replace(".rptdesign", ""),
					birtEngine.openReportDesign(folder.getAbsolutePath() + File.separator + file));
		}
	}

	public List<Report> getReports() {
		List<Report> response = new ArrayList<>();
		for (Map.Entry<String, IReportRunnable> entry : reports.entrySet()) {
			IReportRunnable report = reports.get(entry.getKey());
			IGetParameterDefinitionTask task = birtEngine.createGetParameterDefinitionTask(report);
			Report reportItem = new Report(report.getDesignHandle().getProperty("title").toString(), entry.getKey());
			for (Object h : task.getParameterDefns(false)) {
				IParameterDefn def = (IParameterDefn) h;
				reportItem.getParameters().add(new Report.Parameter(def.getPromptText(), def.getName(), getParameterType(def)));
			}
			response.add(reportItem);
		}
		return response;
	}

	private Report.ParameterType getParameterType(IParameterDefn param) {
		if (IParameterDefn.TYPE_INTEGER == param.getDataType()) {
			return Report.ParameterType.INT;
		}
		return Report.ParameterType.STRING;
	}

	public void generateMainReport(String reportName, OutputType output, HttpServletResponse response, HttpServletRequest request) {
		switch (output) {
		case HTML:
			response.setContentType(birtEngine.getMIMEType("html"));

			try {
				Map<String, Object> properties = new HashMap<>();
//				properties.put(EngineConstants.APPCONTEXT_BIRT_VIEWER_HTTPSERVET_REQUEST, request); //@Trifon- works without this line
				
				generateHTMLReport(reports.get(reportName), response.getOutputStream(), properties);
			} catch (Exception ex) {
				throw new RuntimeException(ex.getMessage(), ex);
			}
			break;
		case PDF:
			response.setContentType(birtEngine.getMIMEType("pdf"));

			try {
				Map<String, Object> properties = new HashMap<>();
//				properties.put(EngineConstants.APPCONTEXT_PDF_RENDER_CONTEXT, request); //@Trifon- works without this line

				generatePDFReport(reports.get(reportName), response.getOutputStream(), properties);
			} catch (Exception ex) {
				throw new RuntimeException(ex.getMessage(), ex);
			}
			break;
		case FO:
			response.setContentType(birtEngine.getMIMEType("txt"));

			try {
				Map<String, Object> properties = new HashMap<>();
//				properties.put(EngineConstants.APPCONTEXT_PDF_RENDER_CONTEXT, request); //@Trifon- works without this line

				generatePDFReport(reports.get(reportName), response.getOutputStream(), properties);
			} catch (Exception ex) {
				throw new RuntimeException(ex.getMessage(), ex);
			}
			break;

		default:
			throw new IllegalArgumentException("Output type not recognized:" + output);
		}
	}

	/**
	 * Generate a report as HTML
	 */
	@SuppressWarnings("unchecked")
//	private void generateHTMLReport(IReportRunnable report, HttpServletResponse response, HttpServletRequest request) {
	private void generateHTMLReport(IReportRunnable report, OutputStream outputStream, Map<String, Object> properties) {
		IRunAndRenderTask runAndRenderTask = birtEngine.createRunAndRenderTask(report);
//		response.setContentType(birtEngine.getMIMEType("html")); //@Trifon
		IRenderOption options = new RenderOption();
		HTMLRenderOption htmlOptions = new HTMLRenderOption(options);
		htmlOptions.setOutputFormat("html");
		htmlOptions.setBaseImageURL("/" + reportsPath + imagesPath);
		htmlOptions.setImageDirectory(imageFolder);
		htmlOptions.setImageHandler(htmlImageHandler);
		runAndRenderTask.setRenderOption(htmlOptions);

		runAndRenderTask.getAppContext().putAll(properties);

		try {
			htmlOptions.setOutputStream(outputStream); //@Trifon; old: response.getOutputStream()
			runAndRenderTask.run();
		} catch (Exception ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		} finally {
			runAndRenderTask.close();
		}
	}

	/**
	 * Generate a report as PDF
	 */
	@SuppressWarnings("unchecked")
//	private void generatePDFReport(IReportRunnable report, HttpServletResponse response, HttpServletRequest request) {
	private void generatePDFReport(IReportRunnable report, OutputStream outputStream, Map<String, Object> properties) {
		IRunAndRenderTask runAndRenderTask = birtEngine.createRunAndRenderTask(report);
//		response.setContentType(birtEngine.getMIMEType("pdf")); //@Trifon
		IRenderOption options = new RenderOption();
		PDFRenderOption pdfRenderOption = new PDFRenderOption(options);
		pdfRenderOption.setOutputFormat("pdf");
		runAndRenderTask.setRenderOption(pdfRenderOption);

		runAndRenderTask.getAppContext().putAll(properties);

		try {
			pdfRenderOption.setOutputStream(outputStream); //@Trifon - old: response.getOutputStream()
			runAndRenderTask.run();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			runAndRenderTask.close();
		}
	}

	@Override
	public void destroy() {
		birtEngine.destroy();
		Platform.shutdown();
	}
}