package org.metaborg.spoofax.shell.commands;

import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.analysis.AnalyzerFacet;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageDiscoveryRequest;
import org.metaborg.core.language.ILanguageDiscoveryService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.language.LanguageVersion;
import org.metaborg.core.menu.IMenuService;
import org.metaborg.core.project.IProject;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.syntax.ParseException;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.shell.functions.IFunctionFactory;
import org.metaborg.spoofax.shell.invoker.ICommandInvoker;
import org.metaborg.spoofax.shell.output.IResultVisitor;
import org.metaborg.spoofax.shell.output.StyledText;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.google.common.collect.Lists;

/**
 * Test creating and using the {@link LanguageCommand}.
 */
@RunWith(Parameterized.class)
public class LanguageCommandTest {
    private final String extension;

    // Constructor mocks
    @Mock private ILanguageDiscoveryService langDiscoveryService;
    @Mock private IResourceService resourceService;
    @Mock private IMenuService menuService;
    @Mock private ICommandInvoker invoker;
    @Mock private IFunctionFactory functionFactory;

    @Mock private IProject project;
    @Mock private ILanguageImpl lang;
    @Mock private ILanguageComponent langcomp;

    @SuppressWarnings("rawtypes")
    @Mock private CommandBuilder builder;
    @Mock private IResultVisitor visitor;
    @Captor private ArgumentCaptor<StyledText> captor;

    private FileObject langloc;
    private LanguageCommand langCommand;

    /**
     * List the archive types that are tested.
     *
     * @return An array of archive extensions.
     */
    @Parameters
    public static Collection<Object[]> archives() {
        // @formatter.off
        return Arrays.asList(new Object[][] {
                { "zip" },
                { "jar" },
                { "tar" },
                { "tgz" },
                { "tbz2" }
        });
        // @formatter.on
    }

    /**
     * Instantiate a new {@link LanguageCommandTest}.
     *
     * @param archiveExtension
     *            The archive extension to run the tests with, see {@link #archives()}.
     */
    public LanguageCommandTest(String archiveExtension) {
        this.extension = archiveExtension;
    }

    /**
     * Set up mocks used in the test case.
     *
     * @throws FileSystemException
     *             when resolving the temp file fails
     * @throws ParseException
     *             when parsing fails
     */
    @Before
    public void setUp() throws FileSystemException, ParseException {
        initMocks(this);
        langloc = VFS.getManager().resolveFile("res:paplj." + this.extension);
        Mockito.<Iterable<? extends ILanguageImpl>>when(langcomp.contributesTo())
            .thenReturn(Lists.newArrayList(lang));
        when(resourceService.resolveToName(anyString())).thenReturn(langloc.getName());
        when(lang.id()).thenReturn(new LanguageIdentifier("org.borg", "lang",
                                                          new LanguageVersion(0, 0, 0, "snap")));

        when(functionFactory.createBuilder(any(), any())).thenAnswer((invocation) -> builder);
        when(builder.description(anyString())).thenReturn(builder);
        when(builder.parse()).thenReturn(builder);
        when(builder.analyze()).thenReturn(builder);
        when(builder.transformParsed(any())).thenReturn(builder);
        when(builder.transformAnalyzed(any())).thenReturn(builder);
        when(builder.evalParsed()).thenReturn(builder);
        when(builder.evalAnalyzed()).thenReturn(builder);
        when(builder.evalPOpen()).thenReturn(builder);
        when(builder.evalAOpen()).thenReturn(builder);
        when(builder.style()).thenReturn(builder);

        langCommand = new LanguageCommand(langDiscoveryService, resourceService, menuService,
                                          invoker, functionFactory, project);
    }

    /**
     * Verify that the description of a command is never null.
     */
    @Test
    public void testDescription() {
        assertThat(langCommand.description(), isA(String.class));
    }

    /**
     * Test parsing source that results in a valid {@link ISpoofaxParseUnit}.
     * @throws MetaborgException when language discovery fails
     * @throws FileSystemException when the file could not be found
     */
    @Test(expected = MetaborgException.class)
    public void testLoadLanguageFail() throws MetaborgException, FileSystemException {
        Iterable<ILanguageDiscoveryRequest> langrequest = any();
        when(langDiscoveryService.discover(langrequest)).thenReturn(Lists.newArrayList());

        langCommand.load(langloc);
    }

    /**
     * Test parsing source that results in a valid {@link ISpoofaxParseUnit}.
     * @throws MetaborgException when language discovery fails
     * @throws FileSystemException when the file could not be found
     */
    @Test
    public void testLoadLanguage() throws MetaborgException, FileSystemException {
        Iterable<ILanguageDiscoveryRequest> langrequest = anyCollectionOf(ILanguageDiscoveryRequest.class);
        when(langDiscoveryService.discover(langrequest)).thenReturn(Lists.newArrayList(langcomp));

        ILanguageImpl actual = langCommand.load(langloc);
        verify(langDiscoveryService, times(1)).request(langloc);
        verify(langDiscoveryService, times(1)).discover(langrequest);
        assertEquals(lang, actual);
    }

    /**
     * Test execute with invalid input arguments.
     * @throws MetaborgException when language discovery fails
     */
    public void testExecuteInvalidArgs1() throws MetaborgException {
        Iterable<ILanguageDiscoveryRequest> langrequest = any();
        when(langDiscoveryService.discover(langrequest)).thenReturn(Lists.newArrayList(langcomp));

        langCommand.execute().accept(visitor);
        verify(visitor, times(1)).visitException(any(MetaborgException.class));
    }

    /**
     * Test execute with invalid input arguments.
     *
     * @throws MetaborgException
     *             when language discovery fails
     */
    @Test
    public void testExecuteInvalidArgs2() throws MetaborgException {
        Iterable<ILanguageDiscoveryRequest> langrequest = any();
        when(langDiscoveryService.discover(langrequest)).thenReturn(Lists.newArrayList(langcomp));

        langCommand.execute(new String[] { "", "" }).accept(visitor);

        verify(visitor, times(1)).visitException(any(MetaborgException.class));
    }

    /**
     * Test execute with valid input arguments and without AnalysisFacet.
     *
     * @throws MetaborgException
     *             when language discovery fails
     */
    @Test
    public void testExecute() throws MetaborgException {
        Iterable<ILanguageDiscoveryRequest> langrequest = any();
        when(langDiscoveryService.discover(langrequest)).thenReturn(Lists.newArrayList(langcomp));
        when(menuService.menuItems(any())).thenReturn(Lists.newArrayList());

        String expected = "Loaded language org.borg:lang:0.0.0-snap";
        langCommand.execute("res:paplj.zip").accept(visitor);
        verify(visitor, times(1)).visitMessage(captor.capture());
        verify(invoker, times(1)).resetCommands();
        verify(invoker, atLeast(1)).addCommand(any(), any());
        verify(invoker, never()).addCommand(eq("analyze"), any());
        assertEquals(expected, captor.getValue().toString());
    }

    /**
     * Test execute with valid input arguments and with AnalysisFacet.
     * @throws MetaborgException when language discovery fails
     */
    @Test
    public void testExecuteAnalyzed() throws MetaborgException {
        Iterable<ILanguageDiscoveryRequest> langrequest = any();
        when(langDiscoveryService.discover(langrequest)).thenReturn(Lists.newArrayList(langcomp));
        when(menuService.menuItems(any())).thenReturn(Lists.newArrayList());
        when(lang.hasFacet(AnalyzerFacet.class)).thenReturn(true);

        String expected = "Loaded language org.borg:lang:0.0.0-snap";
        langCommand.execute("res:paplj.zip").accept(visitor);
        verify(visitor, times(1)).visitMessage(captor.capture());
        verify(invoker, times(1)).resetCommands();
        verify(invoker, atLeast(1)).addCommand(any(), any());
        verify(invoker, times(1)).addCommand(eq("analyze"), any());
        assertEquals(expected, captor.getValue().toString());
    }

}
