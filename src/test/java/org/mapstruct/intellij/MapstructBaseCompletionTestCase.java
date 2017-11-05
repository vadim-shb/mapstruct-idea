/*
 *  Copyright 2017 the MapStruct authors (http://www.mapstruct.org/)
 *  and/or other contributors as indicated by the @authors tag. See the
 *  copyright.txt file in the distribution for a full listing of all
 *  contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.mapstruct.intellij;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.intellij.codeInsight.completion.LightFixtureCompletionTestCase;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;

import static com.intellij.testFramework.LightPlatformTestCase.getModule;

/**
 * Base completion test case for MapStruct.
 *
 * @author Filip Hrisafov
 */
public abstract class MapstructBaseCompletionTestCase extends LightFixtureCompletionTestCase {

    private static final String BUILD_LIBS_DIRECTORY = "build/libs";
    private static final String BUILD_MOCK_JDK_DIRECTORY = "build/mockJDK-";

    private Ref<Library> ref = Ref.create();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final String mapstructLibPath = PathUtil.toSystemIndependentName( new File( BUILD_LIBS_DIRECTORY )
            .getAbsolutePath() );
        VfsRootAccess.allowRootAccess( mapstructLibPath );
        Module module = getModule();
        ref.set( addLibrary( module, "Mapstruct", mapstructLibPath, "mapstruct.jar" ) );
    }

    /**
     * This is exactly the same as
     * {@link PsiTestUtil#addLibrary(Module, ModifiableRootModel, String, String, String...)}
     * with the only difference that this one returns a {@link Library}. This is needed, because in 2017.3
     * the tests need to be disposed. However, the needed function does not return {@link Library}.
     */
    private static Library addLibrary(Module module,
        String libName,
        String libPath,
        String... jarArr) {
        List<VirtualFile> classesRoots = new ArrayList<>();
        for ( String jar : jarArr ) {
            if ( !libPath.endsWith( "/" ) && !jar.startsWith( "/" ) ) {
                jar = "/" + jar;
            }
            String path = libPath + jar;
            VirtualFile root;
            if ( path.endsWith( ".jar" ) ) {
                root = JarFileSystem.getInstance().refreshAndFindFileByPath( path + "!/" );
            }
            else {
                root = LocalFileSystem.getInstance().refreshAndFindFileByPath( path );
            }
            assert root != null : "Library root folder not found: " + path + "!/";
            classesRoots.add( root );
        }
        return PsiTestUtil.addProjectLibrary( module, libName, classesRoots, Collections.emptyList() );
    }

    @Override
    public void tearDown() throws Exception {
        // Setup for tearing down and setting up library comes from
        // https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000648324
        if ( !ref.isNull() ) {
            removeLibrary( ref.get() );
        }
        super.tearDown();
    }

    private void removeLibrary(Library library) {
        WriteCommandAction.runWriteCommandAction( null, () -> {
            LibraryTable table = ProjectLibraryTable.getInstance( getProject() );
            LibraryTable.ModifiableModel model = table.getModifiableModel();
            model.removeLibrary( library );
            model.commit();

        } );
    }

    protected void addDirectoryToProject(@NotNull String directory) {
        myFixture.copyDirectoryToProject( directory, StringUtil.getShortName( directory, '/' ) );
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        LanguageLevel languageLevel = getLanguageLevel();
        return new DefaultLightProjectDescriptor() {
            @Override
            public Sdk getSdk() {
                String compilerOption = languageLevel.getCompilerComplianceDefaultOption();
                return JavaSdk.getInstance()
                    .createJdk( "java " + compilerOption, BUILD_MOCK_JDK_DIRECTORY + compilerOption, false );
            }

            @Override
            public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model,
                @NotNull ContentEntry contentEntry) {
                model.getModuleExtension( LanguageLevelModuleExtension.class )
                    .setLanguageLevel( languageLevel );
            }
        };
    }

    protected LanguageLevel getLanguageLevel() {
        return LanguageLevel.JDK_1_7;
    }
}
