package com.cflint.api;

import com.cflint.BugInfo;
import com.cflint.BugList;
import com.cflint.CFLint;
import com.cflint.Version;
import com.cflint.config.CFLintConfiguration;
import com.cflint.config.CFLintPluginInfo;
import com.cflint.config.CFLintPluginInfo.RuleGroup;
import com.cflint.config.ConfigBuilder;
import com.cflint.config.ConfigUtils;
import com.cflint.exception.CFLintConfigurationException;
import com.cflint.exception.CFLintScanException;
import com.cflint.tools.CFLintFilter;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides a public API for integrating CFLint directly into another JVM environment.
 */
public class CFLintAPI {

    private final CFLintPluginInfo pluginInfo = ConfigUtils.loadDefaultPluginInfo();

    private String filterFile = null;
    private boolean verbose = false;
    private boolean logError = false;
    private boolean quiet = false;
    private boolean debug = false;

    /**
     * List of file extensions to scan.  Default to *.cfc and *.cfm
     */
    private List<String> extensions = new ArrayList<>(Arrays.asList("cfc", "cfm"));

    private boolean strictInclude;

    private final CFLintConfiguration configuration;
    private final CFLint cflint;

    public CFLintAPI(final CFLintConfiguration configuration) throws CFLintConfigurationException {
        super();
        this.configuration = configuration;
        this.cflint = createCFLintInstance();
    }

    public CFLintAPI() throws CFLintConfigurationException {
        this(new ConfigBuilder().build());
    }


    public CFLintResult scan(final List<String> fileOrFolder) {
        for (final String scanFolder : fileOrFolder) {
            cflint.scan(scanFolder);
        }

        for (final BugInfo bug : cflint.getBugs()) {
            cflint.getStats().getCounts().add(bug.getMessageCode(), bug.getSeverity());
        }

        return new CFLintResult(cflint);
    }

    public CFLintResult scan(final String source) throws CFLintScanException {
        return scan(source, "source.cfc");
    }

    public CFLintResult scan(final String source, final String filename) throws CFLintScanException {
        final File starterFile = new File(filename);
        if (starterFile.exists() && starterFile.getParentFile().exists()) {
            cflint.setupConfigAncestry(starterFile.getParentFile());
        }

        cflint.process(source, filename);
        for (final BugInfo bug : cflint.getBugs()) {
            cflint.getStats().getCounts().add(bug.getMessageCode(), bug.getSeverity());
        }

        return new CFLintResult(cflint);
    }

    public BugList getResults() {
        return cflint.getBugs();
    }

    private CFLint createCFLintInstance() throws CFLintConfigurationException {
        try {
            final CFLint cflint = new CFLint(configuration);
            cflint.setVerbose(verbose);
            cflint.setLogError(logError);
            cflint.setQuiet(quiet);
            cflint.setDebug(debug);
            cflint.setStrictIncludes(strictInclude);
            cflint.setAllowedExtensions(extensions);
            if (filterFile != null) {
                cflint.getBugs().setFilter(createFilter());
            }
            return cflint;
        } catch (final Exception e) {
            throw new CFLintConfigurationException(e);
        }
    }

    protected CFLintFilter createFilter() throws CFLintConfigurationException {
        try {
            if (filterFile != null) {
                final File file = new File(filterFile);
                if (file.exists()) {
                    final FileInputStream fis = new FileInputStream(file);
                    final byte b[] = new byte[fis.available()];
                    IOUtils.read(fis, b);
                    fis.close();
                    return CFLintFilter.createFilter(new String(b), verbose);
                }
            }
            return CFLintFilter.createFilter(verbose);
        } catch (final Exception e) {
            throw new CFLintConfigurationException(e);
        }
    }

    /**
     * Return the current version of CFLint
     *
     * @return the current version of CFLint
     */
    public String getVersion() {
        return Version.getVersion();
    }

    /**
     * Return the version of CFParser used by the current CFLint
     *
     * @return the version of CFParser used by this version of CFLint
     */
    public String getCFParserVersion() {
        return cfml.parsing.Version.getVersion();
    }

    /**
     * List the rule groups
     *
     * @return the list of rule groups
     */
    public List<RuleGroup> getRuleGroups() {
        return pluginInfo.getRuleGroups();
    }

    /**
     * Limit file extensions to this list
     *
     * @param extensions list of allowed extensions
     */
    public void setExtensions(final List<String> extensions) {
        this.extensions = extensions;
        if (cflint != null) {
            cflint.setAllowedExtensions(extensions);
        }
    }

    /**
     * Verbose output
     *
     * @param verbose verbose output
     */
    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
        if (cflint != null) {
            cflint.setVerbose(verbose);
        }
    }

    /**
     * Log errors to standard error.
     *
     * @param logError log errors to standard error
     */
    public void setLogError(final boolean logError) {
        this.logError = logError;
        if (cflint != null) {
            cflint.setLogError(logError);
        }
    }

    /**
     * Run quietly
     *
     * @param quiet run quietly
     */
    public void setQuiet(final boolean quiet) {
        this.quiet = quiet;
        if (cflint != null) {
            cflint.setQuiet(quiet);
        }
    }

    /**
     * Run in debug mode
     *
     * @param debug run quietly
     */
    public void setDebug(final boolean debug) {
        this.debug = debug;
        if (cflint != null) {
            cflint.setDebug(debug);
        }
    }

    /**
     * Follow include paths and report an error if the included file cannot be
     * found
     *
     * @param strictInclude strict include
     */
    public void setStrictInclude(final boolean strictInclude) {
        this.strictInclude = strictInclude;
        if (cflint != null) {
            cflint.setStrictIncludes(strictInclude);
        }
    }

    /**
     * Get the configuration object used by the API
     *
     * @return the configuration object
     */
    public CFLintConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Set filter file
     *
     * @param filterFile filter file
     */
    public void setFilterFile(final String filterFile) throws CFLintConfigurationException {
        this.filterFile = filterFile;
        if (cflint != null) {
            cflint.getBugs().setFilter(createFilter());
        }
    }

}
